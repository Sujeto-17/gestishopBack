package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.com.gestishop.application.dto.request.CancelarPeriodoRequestDTO;
import mx.com.gestishop.application.dto.request.SuscripcionRequestDTO;
import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.application.mapper.SuscripcionMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.SuscripcionService;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.model.Suscripcion;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.SuscripcionRepository;
import mx.com.gestishop.infrastructure.utils.FechaNegocioUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ciclo de vida de la suscripción de un negocio:
 * <pre>
 *   alta -> PRUEBA (15 días, sin costo) -> termina -> negocio 'inactivo' (sin acceso)
 *                                                  -> el cliente paga -> superadmin "Registrar pago"
 *                                                  -> negocio 'activo' hasta el fin del periodo pagado (30 días)
 * </pre>
 * Cada pago queda como una fila propia (historial). negocios.vencimiento y negocios.estatus se calculan SIEMPRE
 * con la misma regla (AccesoNegocioCalculator) a partir de esos periodos, y son lo que el login revisa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuscripcionServiceImpl implements SuscripcionService {

    // Tope de la columna suscripciones.notas
    private static final int MAX_NOTAS = 255;

    private final SuscripcionRepository suscripcionRepository;
    private final NegocioRepository negocioRepository;

    // ------------------------------------------------------------------ alta de negocio
    // Crea el periodo de prueba al registrar un negocio (misma transacción que el alta del negocio)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Suscripcion crearPeriodoInicial(Negocio negocio, Plan plan) {
        Suscripcion periodo = SuscripcionMapper.crearPeriodoPrueba(negocio, plan);
        return RepositoryExecutor.execute(
                () -> suscripcionRepository.save(periodo), "Suscripcion", "crearPeriodoInicial");
    }

    // ------------------------------------------------------------------ pagos
    /**
     * El cliente pagó: se crea el periodo de 30 días ya pagado y se le devuelve el acceso al negocio.
     * Si todavía le quedaban días, el nuevo periodo empieza donde termina el actual (no se pierden días).
     * Un negocio suspendido a mano también se reactiva: registrar su pago es darle acceso otra vez.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SuscripcionResponseDTO registrarPago(Long idNegocio, SuscripcionRequestDTO dto) {
        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "registrarPago"
        );

        LocalDate inicio = AccesoNegocioCalculator.inicioDelNuevoPeriodo(periodosDe(idNegocio), FechaNegocioUtil.hoy());

        Suscripcion nuevo = SuscripcionMapper.crearPeriodoPagado(
                negocio, negocio.getPlan(), inicio, dto.getFechaPago(), dto.getMetodoPago(), recortar(dto.getNotas()));

        Suscripcion guardado = RepositoryExecutor.execute(
                () -> suscripcionRepository.save(nuevo), "Suscripcion", "registrarPago");

        recalcularAcceso(negocio, true);

        return SuscripcionMapper.toResponse(guardado);
    }

    /**
     * Deshace un periodo pagado por error. Solo se pueden cancelar periodos 'pagada': la prueba no se
     * cancela (si hay que terminarla antes, se suspende el negocio). El acceso se recalcula con lo que queda.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SuscripcionResponseDTO cancelarPeriodo(Long idSuscripcion, CancelarPeriodoRequestDTO dto) {
        Suscripcion periodo = RepositoryExecutor.execute(
                () -> suscripcionRepository.findById(idSuscripcion)
                        .orElseThrow(() -> ErrorFactory.notFound("Suscripcion", idSuscripcion)),
                "Suscripcion", "cancelarPeriodo"
        );

        if (!"pagada".equals(periodo.getEstatus())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Solo se pueden cancelar periodos pagados."));
        }

        periodo.setEstatus("cancelada");
        periodo.setNotas(recortar(textoNotas(periodo.getNotas()) + "Cancelado: " + dto.getMotivo().trim()));

        Suscripcion guardado = RepositoryExecutor.execute(
                () -> suscripcionRepository.save(periodo), "Suscripcion", "cancelarPeriodo");

        // false = si el superadmin había suspendido el negocio a mano, cancelar un pago no lo reactiva
        recalcularAcceso(periodo.getNegocio(), false);

        return SuscripcionMapper.toResponse(guardado);
    }

    // ------------------------------------------------------------------ consultas
    @Override
    public List<SuscripcionResponseDTO> listarPorNegocio(Long idNegocio) {
        return RepositoryExecutor.execute(
                        () -> suscripcionRepository.listarPorNegocio(idNegocio), "Suscripcion", "listarPorNegocio")
                .stream().map(SuscripcionMapper::toResponse).toList();
    }

    // ------------------------------------------------------------------ proceso automático
    /**
     * Corre cada noche y al arrancar la app (ver VencimientoScheduler). Mantiene al día los estatus del panel;
     * el corte real del acceso no depende de este proceso: lo hace el login comparando la fecha.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int procesarVencimientos() {
        int negocios = RepositoryExecutor.execute(
                () -> negocioRepository.desactivarVencidos(FechaNegocioUtil.hoy(), OffsetDateTime.now()),
                "Negocio", "desactivarVencidos");

        if (negocios > 0) {
            log.info("Vencimientos procesados: {} negocio(s) desactivado(s)", negocios);
        }
        return negocios;
    }

    // ------------------------------------------------------------------ interno
    // Los periodos del negocio reducidos a lo que necesita la regla de acceso
    private List<AccesoNegocioCalculator.Periodo> periodosDe(Long idNegocio) {
        return RepositoryExecutor.execute(
                        () -> suscripcionRepository.listarPeriodosConAcceso(idNegocio), "Suscripcion", "listarPeriodos")
                .stream()
                .map(s -> new AccesoNegocioCalculator.Periodo(s.getEstatus(), s.getFechaInicio(), s.getFechaVencimiento()))
                .toList();
    }

    /**
     * Deja el vencimiento y el estatus del negocio como los dicta la regla de acceso.
     *
     * @param reactivar true al registrar un pago (siempre queda con acceso); false al cancelar: si el
     *                  negocio estaba suspendido a mano (inactivo con fecha vigente), conserva su suspensión
     */
    private void recalcularAcceso(Negocio negocio, boolean reactivar) {
        LocalDate hoy = FechaNegocioUtil.hoy();
        AccesoNegocioCalculator.Resultado resultado = AccesoNegocioCalculator.calcular(periodosDe(negocio.getIdNegocio()), hoy);

        boolean suspendidoAMano = "inactivo".equals(negocio.getEstatus()) && !negocio.getVencimiento().isBefore(hoy);

        negocio.setVencimiento(resultado.vencimiento());
        if (reactivar || !suspendidoAMano || resultado.vencimiento().isBefore(hoy)) {
            negocio.setEstatus(resultado.estatusNegocio());
        }

        RepositoryExecutor.executeVoid(() -> negocioRepository.save(negocio), "Negocio", "recalcularAcceso");
    }

    // Notas existentes seguidas de un separador, para agregarles texto sin perderlas
    private String textoNotas(String notas) {
        return notas == null || notas.isBlank() ? "" : notas.trim() + " | ";
    }

    // Respeta el tope de la columna (255) para no fallar por un texto largo
    private String recortar(String texto) {
        if (texto == null) return null;
        return texto.length() > MAX_NOTAS ? texto.substring(0, MAX_NOTAS) : texto;
    }
}
