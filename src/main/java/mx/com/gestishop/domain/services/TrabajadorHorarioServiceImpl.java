package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AsignarHorarioRequestDTO;
import mx.com.gestishop.application.dto.response.HorarioVigenteTrabajadorResponseDTO;
import mx.com.gestishop.application.dto.response.TrabajadorHorarioResponseDTO;
import mx.com.gestishop.application.mapper.HorarioMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.TrabajadorHorarioService;
import mx.com.gestishop.domain.model.CatTolerancia;
import mx.com.gestishop.domain.model.Horario;
import mx.com.gestishop.domain.model.Trabajador;
import mx.com.gestishop.domain.model.TrabajadorHorario;
import mx.com.gestishop.domain.repository.CatToleranciaRepository;
import mx.com.gestishop.domain.repository.HorarioRepository;
import mx.com.gestishop.domain.repository.TrabajadorHorarioRepository;
import mx.com.gestishop.domain.repository.TrabajadorRepository;
import mx.com.gestishop.infrastructure.utils.FechaNegocioUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Asigna horarios a los trabajadores conservando el historial: nunca se reescribe un horario pasado;
 * al cambiar, el vigente se cierra el día anterior a la nueva fecha y se abre uno nuevo.
 * Un trabajador solo puede usar horarios globales o de su propio negocio, y que estén activos.
 */
@Service
@RequiredArgsConstructor
public class TrabajadorHorarioServiceImpl implements TrabajadorHorarioService {

    private final TrabajadorRepository trabajadorRepository;
    private final TrabajadorHorarioRepository trabajadorHorarioRepository;
    private final HorarioRepository horarioRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorHorarioResponseDTO asignar(UUID uuidUsuario, AsignarHorarioRequestDTO dto, Long idNegocio) {
        Trabajador trabajador = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuarioYNegocio(uuidUsuario, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "consultar");

        TrabajadorHorario nuevo = abrirNuevoHorario(trabajador, dto, FechaNegocioUtil.hoy());
        return HorarioMapper.toResponse(nuevo, FechaNegocioUtil.hoy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void asignarInicial(Trabajador trabajador, AsignarHorarioRequestDTO dto) {
        // Al dar de alta, si no se indica fecha, el horario rige desde el ingreso del trabajador
        LocalDate porDefecto = trabajador.getFechaIngreso() != null ? trabajador.getFechaIngreso() : FechaNegocioUtil.hoy();
        abrirNuevoHorario(trabajador, dto, porDefecto);
    }

    @Override
    public List<HorarioVigenteTrabajadorResponseDTO> vigentesDelNegocio(Long idNegocio) {
        return RepositoryExecutor.execute(
                        () -> trabajadorHorarioRepository.vigentesDelNegocio(idNegocio, FechaNegocioUtil.hoy()),
                        "TrabajadorHorario", "vigentesDelNegocio")
                .stream().map(HorarioMapper::toVigente).toList();
    }

    @Override
    public List<TrabajadorHorarioResponseDTO> historial(UUID uuidUsuario, Long idNegocio) {
        Trabajador trabajador = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuarioYNegocio(uuidUsuario, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "consultar");

        LocalDate hoy = FechaNegocioUtil.hoy();
        return RepositoryExecutor.execute(
                        () -> trabajadorHorarioRepository.historial(trabajador.getIdTrabajador()),
                        "TrabajadorHorario", "historial")
                .stream().map(th -> HorarioMapper.toResponse(th, hoy)).toList();
    }

    /**
     * Valida y registra el nuevo horario. Si el trabajador ya tiene uno abierto, lo cierra el día anterior.
     * El cierre se guarda y se envía a la BD (flush) ANTES de insertar el nuevo: si no, durante un instante
     * los dos rangos de fechas se traslapan y la restricción de la BD lo rechazaría.
     */
    private TrabajadorHorario abrirNuevoHorario(Trabajador trabajador, AsignarHorarioRequestDTO dto, LocalDate fechaPorDefecto) {
        Long idNegocio = trabajador.getNegocio().getIdNegocio();
        LocalDate desde = dto.getVigenteDesde() != null ? dto.getVigenteDesde() : fechaPorDefecto;

        Horario horario = RepositoryExecutor.execute(
                () -> horarioRepository.buscarVisiblePorUuid(dto.getUuidHorario(), idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Horario", dto.getUuidHorario())),
                "Horario", "consultar");

        if (!"activo".equals(horario.getEstatus())) {
            throw conflicto("El horario \"" + horario.getNombre() + "\" está cancelado y no se puede asignar.");
        }

        Optional<TrabajadorHorario> abierto = RepositoryExecutor.execute(
                () -> trabajadorHorarioRepository.buscarAbierto(trabajador.getIdTrabajador()),
                "TrabajadorHorario", "consultarVigente");

        if (abierto.isPresent()) {
            TrabajadorHorario actual = abierto.get();

            if (actual.getHorario().getIdHorario().equals(horario.getIdHorario())) {
                throw conflicto("El trabajador ya tiene ese horario vigente.");
            }
            if (!desde.isAfter(actual.getVigenteDesde())) {
                throw conflicto("La fecha debe ser posterior al inicio del horario actual ("
                        + actual.getVigenteDesde() + ").");
            }

            actual.setVigenteHasta(desde.minusDays(1));
            RepositoryExecutor.executeVoid(() -> trabajadorHorarioRepository.saveAndFlush(actual),
                    "TrabajadorHorario", "cerrarVigente");
        } else {
            // Sin horario abierto: el nuevo no puede empezar antes del fin del último registrado
            LocalDate ultimoFin = RepositoryExecutor.execute(
                    () -> trabajadorHorarioRepository.ultimaFechaFin(trabajador.getIdTrabajador()),
                    "TrabajadorHorario", "consultarHistorial");
            if (ultimoFin != null && !desde.isAfter(ultimoFin)) {
                throw conflicto("La fecha se traslapa con el historial del trabajador (último horario hasta "
                        + ultimoFin + ").");
            }
        }

        TrabajadorHorario nuevo = TrabajadorHorario.builder()
                .trabajador(trabajador)
                .horario(horario)
                .vigenteDesde(desde)
                .build();

        return RepositoryExecutor.execute(
                () -> trabajadorHorarioRepository.save(nuevo), "TrabajadorHorario", "asignar");
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
