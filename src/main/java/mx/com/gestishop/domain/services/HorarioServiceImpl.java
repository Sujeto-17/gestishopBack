package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.HorarioDiaRequestDTO;
import mx.com.gestishop.application.dto.request.HorarioRequestDTO;
import mx.com.gestishop.application.dto.response.HorarioResponseDTO;
import mx.com.gestishop.application.dto.response.ToleranciaResponseDTO;
import mx.com.gestishop.application.mapper.HorarioMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.HorarioService;
import mx.com.gestishop.domain.model.Horario;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.repository.CatToleranciaRepository;
import mx.com.gestishop.domain.repository.HorarioRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.TrabajadorHorarioRepository;
import mx.com.gestishop.infrastructure.utils.FechaNegocioUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reglas del módulo Horarios:
 *  - Los horarios globales (sin negocio) no se editan, cancelan ni eliminan.
 *  - Un horario propio se puede editar o eliminar solo mientras nadie lo haya usado.
 *  - Una vez usado queda bloqueado: solo se cancela, y únicamente si ningún trabajador lo tiene vigente.
 *  - No se permiten dos horarios iguales (mismos días y horas) ni nombres repetidos, contando los globales.
 * La BD tiene triggers que aplican estas mismas reglas como última defensa.
 */
@Service
@RequiredArgsConstructor
public class HorarioServiceImpl implements HorarioService {

    private static final Long SIN_EXCLUIR = -1L;   // al crear no hay horario propio que excluir en las comparaciones
    private static final String[] NOMBRE_DIA = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private final HorarioRepository horarioRepository;
    private final TrabajadorHorarioRepository trabajadorHorarioRepository;
    private final CatToleranciaRepository catToleranciaRepository;
    private final NegocioRepository negocioRepository;

    // --- CONSULTAS ---

    @Override
    public List<HorarioResponseDTO> listar(Long idNegocio, boolean soloActivos) {
        List<Horario> horarios = RepositoryExecutor.execute(
                () -> horarioRepository.listarVisibles(idNegocio), "Horario", "listar");

        if (soloActivos) {
            horarios = horarios.stream().filter(h -> "activo".equals(h.getEstatus())).toList();
        }
        return aResponses(horarios);
    }

    @Override
    public HorarioResponseDTO obtener(UUID uuidHorario, Long idNegocio) {
        return aResponse(buscarVisible(uuidHorario, idNegocio));
    }

    @Override
    public List<ToleranciaResponseDTO> listarTolerancias() {
        return RepositoryExecutor.execute(
                        catToleranciaRepository::findAllByOrderByMinutosAsc, "CatTolerancia", "listar")
                .stream().map(HorarioMapper::toResponse).toList();
    }

    // --- ESCRITURA ---

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HorarioResponseDTO crear(HorarioRequestDTO dto, Long idNegocio) {
        validarDias(dto);
        validarNombreLibre(dto.getNombre(), idNegocio, SIN_EXCLUIR);
        validarNoRepetido(dto, idNegocio, SIN_EXCLUIR);

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");

        Horario guardado = RepositoryExecutor.execute(
                () -> horarioRepository.save(HorarioMapper.toEntity(dto, negocio)), "Horario", "crear");

        return HorarioMapper.toResponse(guardado, false, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HorarioResponseDTO actualizar(UUID uuidHorario, HorarioRequestDTO dto, Long idNegocio) {
        Horario horario = buscarVisible(uuidHorario, idNegocio);
        exigirPropio(horario, "modificar");

        if (usadoAlgunaVez(horario)) {
            throw conflicto("Este horario ya fue usado por trabajadores y no se puede modificar. "
                    + "Crea uno nuevo o cancela este.");
        }

        validarDias(dto);
        validarNombreLibre(dto.getNombre(), idNegocio, horario.getIdHorario());
        validarNoRepetido(dto, idNegocio, horario.getIdHorario());

        HorarioMapper.actualizarEntidad(horario, dto);
        Horario guardado = RepositoryExecutor.execute(
                () -> horarioRepository.save(horario), "Horario", "actualizar");

        return HorarioMapper.toResponse(guardado, false, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HorarioResponseDTO cambiarEstatus(UUID uuidHorario, Long idNegocio) {
        Horario horario = buscarVisible(uuidHorario, idNegocio);
        exigirPropio(horario, "cancelar");

        if ("activo".equals(horario.getEstatus())) {
            boolean vigente = RepositoryExecutor.execute(
                    () -> trabajadorHorarioRepository.existeVigente(horario.getIdHorario(), FechaNegocioUtil.hoy()),
                    "TrabajadorHorario", "validarVigentes");
            if (vigente) {
                throw conflicto("No se puede cancelar: hay trabajadores con este horario. "
                        + "Primero cámbiales el horario.");
            }
            horario.setEstatus("inactivo");
        } else {
            horario.setEstatus("activo");
        }

        Horario guardado = RepositoryExecutor.execute(
                () -> horarioRepository.save(horario), "Horario", "cambiarEstatus");
        return aResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminar(UUID uuidHorario, Long idNegocio) {
        Horario horario = buscarVisible(uuidHorario, idNegocio);
        exigirPropio(horario, "eliminar");

        if (usadoAlgunaVez(horario)) {
            throw conflicto("Este horario ya fue usado por trabajadores y no se puede eliminar. "
                    + "Solo se puede cancelar para conservar su historial.");
        }
        RepositoryExecutor.executeVoid(() -> horarioRepository.delete(horario), "Horario", "eliminar");
    }

    // --- VALIDACIONES ---

    // Un día solo puede aparecer una vez y la entrada no puede ser igual a la salida
    private void validarDias(HorarioRequestDTO dto) {
        Set<Integer> vistos = new HashSet<>();
        for (HorarioDiaRequestDTO d : dto.getDias()) {
            String dia = NOMBRE_DIA[d.getDiaSemana()];
            if (!vistos.add(d.getDiaSemana())) {
                throw new ApiResponseException(ApiCodeResponse.BAD_REQUEST,
                        Map.of("detalle", "El día " + dia + " está repetido en el horario."));
            }
            if (d.getHoraEntrada().equals(d.getHoraSalida())) {
                throw new ApiResponseException(ApiCodeResponse.BAD_REQUEST,
                        Map.of("detalle", "El " + dia + " tiene la misma hora de entrada y de salida."));
            }
        }
    }

    // El nombre no puede repetirse entre los horarios globales y los del negocio
    private void validarNombreLibre(String nombre, Long idNegocio, Long excluirId) {
        boolean existe = RepositoryExecutor.execute(
                () -> horarioRepository.existeNombre(nombre.trim(), idNegocio, excluirId),
                "Horario", "validarNombre");
        if (existe) {
            throw conflicto("Ya existe un horario con el nombre \"" + nombre.trim() + "\".");
        }
    }

    // Evita crear un horario con los mismos días y horas que otro que ya existe (global o del negocio)
    private void validarNoRepetido(HorarioRequestDTO dto, Long idNegocio, Long excluirId) {
        String firma = HorarioMapper.firmaDeRequest(dto.getDias());

        List<Horario> visibles = RepositoryExecutor.execute(
                () -> horarioRepository.listarVisibles(idNegocio), "Horario", "validarRepetido");

        visibles.stream()
                .filter(h -> !h.getIdHorario().equals(excluirId))
                .filter(h -> firma.equals(HorarioMapper.firmaDeEntidad(h.getDias())))
                .findFirst()
                .ifPresent(igual -> {
                    String origen = igual.esGlobal() ? "un horario global" : "uno de tus horarios";
                    String cancelado = "inactivo".equals(igual.getEstatus()) ? " (está cancelado; puedes reactivarlo)" : "";
                    throw conflicto("Ya existe " + origen + " con esos mismos días y horas: \""
                            + igual.getNombre() + "\"" + cancelado + ".");
                });
    }

    private void exigirPropio(Horario horario, String accion) {
        if (horario.esGlobal()) {
            throw conflicto("Los horarios globales del sistema no se pueden " + accion + ".");
        }
    }

    private boolean usadoAlgunaVez(Horario horario) {
        return RepositoryExecutor.execute(
                () -> trabajadorHorarioRepository.existsByHorario_IdHorario(horario.getIdHorario()),
                "TrabajadorHorario", "validarUso");
    }

    // --- APOYO ---

    private Horario buscarVisible(UUID uuidHorario, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> horarioRepository.buscarVisiblePorUuid(uuidHorario, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Horario", uuidHorario)),
                "Horario", "consultar");
    }

    private HorarioResponseDTO aResponse(Horario horario) {
        return aResponses(List.of(horario)).get(0);
    }

    // Arma las respuestas calculando, con solo dos consultas, qué horarios se usaron y cuántos trabajadores tiene cada uno vigente
    private List<HorarioResponseDTO> aResponses(List<Horario> horarios) {
        if (horarios.isEmpty()) return List.of();

        List<Long> ids = horarios.stream().map(Horario::getIdHorario).toList();
        LocalDate hoy = FechaNegocioUtil.hoy();

        Set<Long> usados = new HashSet<>(RepositoryExecutor.execute(
                () -> trabajadorHorarioRepository.idsUsados(ids), "TrabajadorHorario", "consultarUsados"));

        Map<Long, Integer> vigentes = RepositoryExecutor.execute(
                        () -> trabajadorHorarioRepository.contarVigentes(ids, hoy), "TrabajadorHorario", "contarVigentes")
                .stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> ((Number) f[1]).intValue()));

        return horarios.stream()
                .map(h -> HorarioMapper.toResponse(h, usados.contains(h.getIdHorario()),
                        vigentes.getOrDefault(h.getIdHorario(), 0)))
                .toList();
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
