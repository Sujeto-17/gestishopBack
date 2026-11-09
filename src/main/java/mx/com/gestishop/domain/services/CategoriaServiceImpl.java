package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AdoptarCategoriasRequestDTO;
import mx.com.gestishop.application.dto.request.CategoriaRequestDTO;
import mx.com.gestishop.application.dto.response.CategoriaResponseDTO;
import mx.com.gestishop.application.dto.response.CategoriaSugeridaResponseDTO;
import mx.com.gestishop.application.mapper.CategoriaMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.CategoriaService;
import mx.com.gestishop.domain.model.CatCategoriaSugerida;
import mx.com.gestishop.domain.model.Categoria;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.repository.CatCategoriaSugeridaRepository;
import mx.com.gestishop.domain.repository.CategoriaRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reglas del módulo Categorías:
 *  - Dos niveles como máximo: familia y subfamilia.
 *  - El nombre no se repite dentro del mismo nivel (sin distinguir mayúsculas).
 *  - Desactivar una familia desactiva sus subfamilias; una subfamilia solo se reactiva si su familia está activa.
 *  - Solo se elimina una categoría sin subfamilias ni productos; en otro caso se desactiva.
 * La BD aplica las mismas reglas de niveles y de aislamiento entre negocios como última defensa.
 */
@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private static final Long SIN_EXCLUIR = -1L;   // al crear no hay categoría que excluir en las comparaciones

    private final CategoriaRepository categoriaRepository;
    private final CatCategoriaSugeridaRepository sugeridaRepository;
    private final NegocioRepository negocioRepository;

    // --- CONSULTAS ---

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listar(Long idNegocio, boolean soloActivas) {
        List<CategoriaResponseDTO> todas = aResponses(listarEntidades(idNegocio), idNegocio);
        return soloActivas
                ? todas.stream().filter(c -> "activo".equals(c.getEstatus())).toList()
                : todas;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO obtener(UUID uuidCategoria, Long idNegocio) {
        return aResponse(buscar(uuidCategoria, idNegocio), idNegocio);
    }

    // --- ESCRITURA ---

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoriaResponseDTO crear(CategoriaRequestDTO dto, Long idNegocio) {
        Categoria padre = resolverPadre(dto.getUuidPadre(), idNegocio, null);
        validarNombreLibre(dto.getNombre(), idNegocio, padre, SIN_EXCLUIR);

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");

        Categoria nueva = Categoria.builder()
                .negocio(negocio)
                .padre(padre)
                .nombre(dto.getNombre().trim())
                .descripcion(limpiar(dto.getDescripcion()))
                .orden(dto.getOrden())
                .build();

        Categoria guardada = RepositoryExecutor.execute(
                () -> categoriaRepository.save(nueva), "Categoria", "crear");
        return aResponse(guardada, idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoriaResponseDTO actualizar(UUID uuidCategoria, CategoriaRequestDTO dto, Long idNegocio) {
        Categoria categoria = buscar(uuidCategoria, idNegocio);
        Categoria padre = resolverPadre(dto.getUuidPadre(), idNegocio, categoria);

        // Una familia con subfamilias no puede pasar a ser subfamilia (excedería los dos niveles)
        if (padre != null && contarHijas(categoria) > 0) {
            throw conflicto("Esta familia tiene subfamilias, por eso no puede quedar dentro de otra.");
        }
        validarNombreLibre(dto.getNombre(), idNegocio, padre, categoria.getIdCategoria());

        categoria.setPadre(padre);
        categoria.setNombre(dto.getNombre().trim());
        categoria.setDescripcion(limpiar(dto.getDescripcion()));
        if (dto.getOrden() != null) categoria.setOrden(dto.getOrden());

        Categoria guardada = RepositoryExecutor.execute(
                () -> categoriaRepository.save(categoria), "Categoria", "actualizar");
        return aResponse(guardada, idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoriaResponseDTO cambiarEstatus(UUID uuidCategoria, Long idNegocio) {
        Categoria categoria = buscar(uuidCategoria, idNegocio);

        if ("activo".equals(categoria.getEstatus())) {
            categoria.setEstatus("inactivo");
            // Al desactivar una familia, sus subfamilias también dejan de estar disponibles
            if (categoria.esRaiz()) {
                RepositoryExecutor.execute(
                                () -> categoriaRepository.findByPadre_IdCategoria(categoria.getIdCategoria()),
                                "Categoria", "listarHijas")
                        .forEach(h -> h.setEstatus("inactivo"));
            }
        } else {
            if (!categoria.esRaiz() && !"activo".equals(categoria.getPadre().getEstatus())) {
                throw conflicto("No se puede reactivar: su familia \"" + categoria.getPadre().getNombre()
                        + "\" está inactiva. Reactiva primero la familia.");
            }
            categoria.setEstatus("activo");
        }

        Categoria guardada = RepositoryExecutor.execute(
                () -> categoriaRepository.save(categoria), "Categoria", "cambiarEstatus");
        return aResponse(guardada, idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminar(UUID uuidCategoria, Long idNegocio) {
        Categoria categoria = buscar(uuidCategoria, idNegocio);

        if (contarHijas(categoria) > 0) {
            throw conflicto("No se puede eliminar: tiene subfamilias. Elimínalas primero o desactiva la categoría.");
        }
        boolean conProductos = RepositoryExecutor.execute(
                () -> categoriaRepository.tieneProductos(categoria.getIdCategoria()), "Categoria", "validarProductos");
        if (conProductos) {
            throw conflicto("No se puede eliminar: tiene productos asignados. Muévelos a otra categoría o desactívala.");
        }
        RepositoryExecutor.executeVoid(() -> categoriaRepository.delete(categoria), "Categoria", "eliminar");
    }

    // --- SUGERIDAS POR GIRO ---

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaSugeridaResponseDTO> listarSugeridas(Long idNegocio) {
        Long idGiro = idGiroDe(idNegocio);
        Set<String> existentes = claves(listarEntidades(idNegocio));

        return RepositoryExecutor.execute(
                        () -> sugeridaRepository.listarPorGiro(idGiro), "CatCategoriaSugerida", "listar")
                .stream()
                .map(s -> CategoriaSugeridaResponseDTO.builder()
                        .idSugerida(s.getIdSugerida())
                        .nombre(s.getNombre())
                        .nombrePadre(s.getPadre() != null ? s.getPadre().getNombre() : null)
                        .yaAdoptada(existentes.contains(clave(
                                s.getPadre() != null ? s.getPadre().getNombre() : null, s.getNombre())))
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CategoriaResponseDTO> adoptarSugeridas(AdoptarCategoriasRequestDTO dto, Long idNegocio) {
        Long idGiro = idGiroDe(idNegocio);

        // Solo cuentan las plantillas que realmente son del giro del negocio
        Map<Long, CatCategoriaSugerida> delGiro = RepositoryExecutor.execute(
                        () -> sugeridaRepository.listarPorGiro(idGiro), "CatCategoriaSugerida", "listar")
                .stream().collect(Collectors.toMap(CatCategoriaSugerida::getIdSugerida, s -> s));

        List<CatCategoriaSugerida> elegidas = dto.getIdsSugeridas().stream().distinct()
                .map(delGiro::get).filter(Objects::nonNull).toList();
        if (elegidas.isEmpty()) {
            throw conflicto("Ninguna de las categorías elegidas corresponde al giro de tu negocio.");
        }

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");

        List<Categoria> existentes = listarEntidades(idNegocio);
        Set<String> claves = claves(existentes);
        Map<String, Categoria> familias = existentes.stream().filter(Categoria::esRaiz)
                .collect(Collectors.toMap(c -> c.getNombre().toLowerCase(), c -> c, (a, b) -> a));
        Set<UUID> creadas = new HashSet<>();

        // 1) Familias: las elegidas y las que necesiten las subfamilias elegidas
        Set<String> nombresFamilias = new LinkedHashSet<>();
        for (CatCategoriaSugerida s : elegidas) {
            nombresFamilias.add(s.getPadre() != null ? s.getPadre().getNombre() : s.getNombre());
        }
        for (String nombre : nombresFamilias) {
            if (familias.containsKey(nombre.toLowerCase())) continue;
            Categoria familia = RepositoryExecutor.execute(
                    () -> categoriaRepository.save(Categoria.builder().negocio(negocio).nombre(nombre).build()),
                    "Categoria", "adoptar");
            familias.put(nombre.toLowerCase(), familia);
            claves.add(clave(null, nombre));
            creadas.add(familia.getUuidCategoria());
        }

        // 2) Subfamilias elegidas (se omiten las que ya existen o cuya familia está inactiva)
        for (CatCategoriaSugerida s : elegidas) {
            if (s.getPadre() == null) continue;
            Categoria familia = familias.get(s.getPadre().getNombre().toLowerCase());
            if (familia == null || !"activo".equals(familia.getEstatus())) continue;
            if (!claves.add(clave(familia.getNombre(), s.getNombre()))) continue;

            Categoria hija = RepositoryExecutor.execute(
                    () -> categoriaRepository.save(
                            Categoria.builder().negocio(negocio).padre(familia).nombre(s.getNombre()).build()),
                    "Categoria", "adoptar");
            creadas.add(hija.getUuidCategoria());
        }

        return listar(idNegocio, false).stream()
                .filter(c -> creadas.contains(c.getUuidCategoria()))
                .toList();
    }

    // --- APOYO ---

    private List<Categoria> listarEntidades(Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> categoriaRepository.listarPorNegocio(idNegocio), "Categoria", "listar");
    }

    private Categoria buscar(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> categoriaRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Categoria", uuid)),
                "Categoria", "consultar");
    }

    // Busca la familia indicada: debe ser del negocio, estar activa, ser de primer nivel y no ser la propia categoría
    private Categoria resolverPadre(UUID uuidPadre, Long idNegocio, Categoria propia) {
        if (uuidPadre == null) return null;
        if (propia != null && propia.getUuidCategoria().equals(uuidPadre)) {
            throw conflicto("Una categoría no puede ser su propia familia.");
        }
        Categoria padre = buscar(uuidPadre, idNegocio);
        if (!padre.esRaiz()) {
            throw conflicto("Solo hay dos niveles: familia y subfamilia. \"" + padre.getNombre()
                    + "\" ya es una subfamilia.");
        }
        if (!"activo".equals(padre.getEstatus())) {
            throw conflicto("La familia \"" + padre.getNombre() + "\" está inactiva.");
        }
        return padre;
    }

    // El nombre no puede repetirse dentro del mismo nivel (entre familias, o entre las subfamilias de una misma familia)
    private void validarNombreLibre(String nombre, Long idNegocio, Categoria padre, Long excluirId) {
        String limpio = nombre.trim();
        boolean existe = RepositoryExecutor.execute(
                () -> padre == null
                        ? categoriaRepository.existeNombreRaiz(idNegocio, limpio, excluirId)
                        : categoriaRepository.existeNombreHija(idNegocio, padre.getIdCategoria(), limpio, excluirId),
                "Categoria", "validarNombre");
        if (existe) {
            throw conflicto(padre == null
                    ? "Ya existe una familia llamada \"" + limpio + "\"."
                    : "Ya existe una subfamilia \"" + limpio + "\" en \"" + padre.getNombre() + "\".");
        }
    }

    private long contarHijas(Categoria categoria) {
        return RepositoryExecutor.execute(
                () -> categoriaRepository.countByPadre_IdCategoria(categoria.getIdCategoria()),
                "Categoria", "contarHijas");
    }

    private Long idGiroDe(Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio))
                        .getGiro().getIdGiro(),
                "Negocio", "consultarGiro");
    }

    // Llave "familia/subfamilia" en minúsculas para comparar categorías equivalentes por nombre
    private String clave(String nombrePadre, String nombre) {
        return (nombrePadre == null ? "" : nombrePadre.trim().toLowerCase()) + "/" + nombre.trim().toLowerCase();
    }

    private Set<String> claves(List<Categoria> categorias) {
        return categorias.stream()
                .map(c -> clave(c.getPadre() != null ? c.getPadre().getNombre() : null, c.getNombre()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private CategoriaResponseDTO aResponse(Categoria categoria, Long idNegocio) {
        return aResponses(List.of(categoria), idNegocio).get(0);
    }

    // Arma las respuestas calculando, con una consulta de productos y conteo en memoria, los totales de cada categoría
    private List<CategoriaResponseDTO> aResponses(List<Categoria> categorias, Long idNegocio) {
        if (categorias.isEmpty()) return List.of();

        Map<Long, Integer> productos = RepositoryExecutor.execute(
                        () -> categoriaRepository.contarProductosPorCategoria(idNegocio), "Categoria", "contarProductos")
                .stream()
                .collect(Collectors.toMap(f -> ((Number) f[0]).longValue(), f -> ((Number) f[1]).intValue()));

        // Las subfamilias se cuentan sobre todas las del negocio, no solo sobre las recibidas
        Map<Long, Integer> hijas = new HashMap<>();
        for (Categoria c : RepositoryExecutor.execute(
                () -> categoriaRepository.listarPorNegocio(idNegocio), "Categoria", "contarHijas")) {
            if (c.getPadre() != null) hijas.merge(c.getPadre().getIdCategoria(), 1, Integer::sum);
        }

        return categorias.stream()
                .map(c -> CategoriaMapper.toResponse(c,
                        hijas.getOrDefault(c.getIdCategoria(), 0),
                        productos.getOrDefault(c.getIdCategoria(), 0)))
                .toList();
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}

