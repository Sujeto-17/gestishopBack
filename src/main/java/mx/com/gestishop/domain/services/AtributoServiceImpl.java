package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AtributoRequestDTO;
import mx.com.gestishop.application.dto.request.AtributoValorRequestDTO;
import mx.com.gestishop.application.dto.response.AtributoResponseDTO;
import mx.com.gestishop.application.dto.response.AtributoValorResponseDTO;
import mx.com.gestishop.application.mapper.AtributoMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.AtributoService;
import mx.com.gestishop.domain.model.Atributo;
import mx.com.gestishop.domain.model.AtributoValor;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.repository.AtributoRepository;
import mx.com.gestishop.domain.repository.AtributoValorRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reglas de atributos:
 *  - El catálogo de atributos es global y solo lo modifica el superadmin.
 *  - Cada negocio puede agregar valores propios a un atributo existente; no puede repetir uno del sistema
 *    ni uno suyo (sin distinguir mayúsculas). Los valores propios solo los ve su negocio.
 *  - Lo que ya está en uso no se borra: se desactiva (deja de ofrecerse pero las variantes existentes lo conservan).
 * La BD aplica las mismas reglas de unicidad y pertenencia como última defensa.
 */
@Service
@RequiredArgsConstructor
public class AtributoServiceImpl implements AtributoService {

    private static final Long SIN_EXCLUIR = -1L;   // al crear no hay registro que excluir en las comparaciones

    private final AtributoRepository atributoRepository;
    private final AtributoValorRepository valorRepository;
    private final NegocioRepository negocioRepository;

    // ================= NEGOCIO =================

    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponseDTO> listarParaNegocio(Long idNegocio) {
        List<AtributoValor> valores = RepositoryExecutor.execute(
                () -> valorRepository.listarVisiblesParaNegocio(idNegocio, true), "AtributoValor", "consultar");
        return armar(atributoRepository.listar(true), valores);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoValorResponseDTO crearValorPropio(UUID uuidAtributo, AtributoValorRequestDTO dto, Long idNegocio) {
        Atributo atributo = buscarAtributo(uuidAtributo);
        if (!"activo".equals(atributo.getEstatus())) {
            throw conflicto("El atributo \"" + atributo.getNombre() + "\" no está disponible");
        }
        String valor = dto.getValor().trim();
        validarValorLibre(atributo, valor, idNegocio, SIN_EXCLUIR);

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");

        AtributoValor nuevo = AtributoValor.builder()
                .atributo(atributo)
                .negocio(negocio)
                .valor(valor)
                .orden(dto.getOrden())
                .build();
        return AtributoMapper.toResponse(RepositoryExecutor.execute(
                () -> valorRepository.save(nuevo), "AtributoValor", "crear"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoValorResponseDTO actualizarValorPropio(UUID uuidValor, AtributoValorRequestDTO dto, Long idNegocio) {
        AtributoValor existente = buscarPropio(uuidValor, idNegocio);
        String valor = dto.getValor().trim();
        validarValorLibre(existente.getAtributo(), valor, idNegocio, existente.getIdValor());

        existente.setValor(valor);
        if (dto.getOrden() != null) existente.setOrden(dto.getOrden());
        return AtributoMapper.toResponse(RepositoryExecutor.execute(
                () -> valorRepository.save(existente), "AtributoValor", "actualizar"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminarValorPropio(UUID uuidValor, Long idNegocio) {
        eliminarOInactivar(buscarPropio(uuidValor, idNegocio));
    }

    // ================= SUPERADMIN =================

    @Override
    @Transactional(readOnly = true)
    public List<AtributoResponseDTO> listarGlobales() {
        List<AtributoValor> valores = RepositoryExecutor.execute(
                valorRepository::listarDelSistema, "AtributoValor", "consultar");
        return armar(atributoRepository.listar(false), valores);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoResponseDTO crearAtributo(AtributoRequestDTO dto) {
        String nombre = dto.getNombre().trim();
        validarNombreLibre(nombre, SIN_EXCLUIR);
        Atributo nuevo = Atributo.builder().nombre(nombre).orden(dto.getOrden()).build();
        Atributo guardado = RepositoryExecutor.execute(
                () -> atributoRepository.save(nuevo), "Atributo", "crear");
        return AtributoMapper.toResponse(guardado, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoResponseDTO actualizarAtributo(UUID uuidAtributo, AtributoRequestDTO dto) {
        Atributo atributo = buscarAtributo(uuidAtributo);
        String nombre = dto.getNombre().trim();
        validarNombreLibre(nombre, atributo.getIdAtributo());

        atributo.setNombre(nombre);
        if (dto.getOrden() != null) atributo.setOrden(dto.getOrden());
        Atributo guardado = RepositoryExecutor.execute(
                () -> atributoRepository.save(atributo), "Atributo", "actualizar");
        return conValoresDelSistema(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoResponseDTO cambiarEstatusAtributo(UUID uuidAtributo) {
        Atributo atributo = buscarAtributo(uuidAtributo);
        atributo.setEstatus("activo".equals(atributo.getEstatus()) ? "inactivo" : "activo");
        Atributo guardado = RepositoryExecutor.execute(
                () -> atributoRepository.save(atributo), "Atributo", "actualizar");
        return conValoresDelSistema(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminarAtributo(UUID uuidAtributo) {
        Atributo atributo = buscarAtributo(uuidAtributo);
        if (RepositoryExecutor.execute(() -> atributoRepository.estaEnUso(atributo.getIdAtributo()),
                "Atributo", "consultar")) {
            atributo.setEstatus("inactivo");
            RepositoryExecutor.execute(() -> atributoRepository.save(atributo), "Atributo", "actualizar");
            return;
        }
        RepositoryExecutor.executeVoid(() -> atributoRepository.delete(atributo), "Atributo", "eliminar");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoValorResponseDTO crearValorGlobal(UUID uuidAtributo, AtributoValorRequestDTO dto) {
        Atributo atributo = buscarAtributo(uuidAtributo);
        String valor = dto.getValor().trim();
        if (valorRepository.existeEnSistema(atributo.getIdAtributo(), valor, SIN_EXCLUIR)) {
            throw conflicto("El valor \"" + valor + "\" ya existe en " + atributo.getNombre());
        }
        AtributoValor nuevo = AtributoValor.builder()
                .atributo(atributo).valor(valor).orden(dto.getOrden()).build();
        return AtributoMapper.toResponse(RepositoryExecutor.execute(
                () -> valorRepository.save(nuevo), "AtributoValor", "crear"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoValorResponseDTO actualizarValorGlobal(UUID uuidValor, AtributoValorRequestDTO dto) {
        AtributoValor existente = buscarDelSistema(uuidValor);
        String valor = dto.getValor().trim();
        if (valorRepository.existeEnSistema(existente.getAtributo().getIdAtributo(), valor, existente.getIdValor())) {
            throw conflicto("El valor \"" + valor + "\" ya existe en " + existente.getAtributo().getNombre());
        }
        existente.setValor(valor);
        if (dto.getOrden() != null) existente.setOrden(dto.getOrden());
        return AtributoMapper.toResponse(RepositoryExecutor.execute(
                () -> valorRepository.save(existente), "AtributoValor", "actualizar"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AtributoValorResponseDTO cambiarEstatusValorGlobal(UUID uuidValor) {
        AtributoValor valor = buscarDelSistema(uuidValor);
        valor.setEstatus("activo".equals(valor.getEstatus()) ? "inactivo" : "activo");
        return AtributoMapper.toResponse(RepositoryExecutor.execute(
                () -> valorRepository.save(valor), "AtributoValor", "actualizar"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminarValorGlobal(UUID uuidValor) {
        eliminarOInactivar(buscarDelSistema(uuidValor));
    }

    // ================= AUXILIARES =================

    // Elimina el valor, o lo desactiva si alguna variante lo usa
    private void eliminarOInactivar(AtributoValor valor) {
        if (RepositoryExecutor.execute(() -> valorRepository.estaEnUso(valor.getIdValor()),
                "AtributoValor", "consultar")) {
            valor.setEstatus("inactivo");
            RepositoryExecutor.execute(() -> valorRepository.save(valor), "AtributoValor", "actualizar");
            return;
        }
        RepositoryExecutor.executeVoid(() -> valorRepository.delete(valor), "AtributoValor", "eliminar");
    }

    // Une cada atributo con sus valores (agrupados por atributo, en el orden recibido)
    private List<AtributoResponseDTO> armar(List<Atributo> atributos, List<AtributoValor> valores) {
        Map<Long, List<AtributoValorResponseDTO>> porAtributo = valores.stream()
                .collect(Collectors.groupingBy(v -> v.getAtributo().getIdAtributo(),
                        Collectors.mapping(AtributoMapper::toResponse, Collectors.toList())));
        return atributos.stream()
                .map(a -> AtributoMapper.toResponse(a, porAtributo.getOrDefault(a.getIdAtributo(), List.of())))
                .toList();
    }

    // Respuesta de un atributo con sus valores del sistema
    private AtributoResponseDTO conValoresDelSistema(Atributo atributo) {
        List<AtributoValorResponseDTO> valores = valorRepository.listarDelSistema().stream()
                .filter(v -> v.getAtributo().getIdAtributo().equals(atributo.getIdAtributo()))
                .map(AtributoMapper::toResponse)
                .toList();
        return AtributoMapper.toResponse(atributo, valores);
    }

    private Atributo buscarAtributo(UUID uuid) {
        return RepositoryExecutor.execute(
                () -> atributoRepository.findByUuidAtributo(uuid)
                        .orElseThrow(() -> ErrorFactory.notFound("Atributo", uuid)),
                "Atributo", "consultar");
    }

    private AtributoValor buscarDelSistema(UUID uuid) {
        return RepositoryExecutor.execute(
                () -> valorRepository.buscarDelSistema(uuid)
                        .orElseThrow(() -> ErrorFactory.notFound("Valor de atributo", uuid)),
                "AtributoValor", "consultar");
    }

    private AtributoValor buscarPropio(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> valorRepository.buscarPropio(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Valor de atributo", uuid)),
                "AtributoValor", "consultar");
    }

    // El nombre del atributo no se repite en el catálogo global
    private void validarNombreLibre(String nombre, Long excluirId) {
        if (atributoRepository.existeNombre(nombre, excluirId)) {
            throw conflicto("Ya existe un atributo llamado \"" + nombre + "\"");
        }
    }

    // Un valor propio no puede repetir uno del sistema ni otro propio del mismo atributo
    private void validarValorLibre(Atributo atributo, String valor, Long idNegocio, Long excluirId) {
        if (valorRepository.existeEnSistema(atributo.getIdAtributo(), valor, SIN_EXCLUIR)) {
            throw conflicto("\"" + valor + "\" ya existe en " + atributo.getNombre() + " (catálogo del sistema)");
        }
        if (valorRepository.existePropio(atributo.getIdAtributo(), idNegocio, valor, excluirId)) {
            throw conflicto("Ya registraste \"" + valor + "\" en " + atributo.getNombre());
        }
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}