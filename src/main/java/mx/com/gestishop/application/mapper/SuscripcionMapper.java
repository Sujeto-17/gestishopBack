package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.model.Suscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Convierte entre la entidad Suscripcion y sus DTOs. Incluye la fábrica
 * para crear el PRIMER periodo de un negocio nuevo (periodo de prueba),
 * usada desde NegocioServiceImpl al dar de alta un negocio.
 */
public class SuscripcionMapper {

    private static final int DIAS_PRUEBA = 15;
    // Duración de un periodo pagado, en días
    public static final int DIAS_PERIODO = 30;

    private SuscripcionMapper() {
    }

    /**
     * Construye la primera suscripción de un negocio recién creado, en estatus 'prueba'.
     * El periodo de prueba NO se cobra: va con precio 0 y no cuenta como pendiente de pago
     * (antes quedaba 'pendiente' con el precio del plan y aparecía como moroso al terminar).
     * Las fechas coinciden exactamente con negocio.fechaAlta/vencimiento,
     * calculadas en NegocioMapper, para no duplicar esa lógica en dos lugares.
     */
    public static Suscripcion crearPeriodoPrueba(Negocio negocio, Plan plan) {
        return Suscripcion.builder()
                .negocio(negocio)
                .plan(plan)
                .precio(BigDecimal.ZERO)
                .fechaInicio(negocio.getFechaAlta())
                .fechaVencimiento(negocio.getVencimiento())
                .estatus("prueba")
                .notas("Periodo de prueba inicial de " + DIAS_PRUEBA + " días")
                .build();
    }

    /**
     * Construye un periodo YA PAGADO de 30 días a partir de la fecha de inicio indicada.
     * El precio es el del plan en este momento y queda guardado en el periodo: si el plan sube de precio
     * después, este historial no cambia. (Antes existía un periodo 'pendiente' que se creaba al renovar
     * y se cobraba aparte; ahora todo periodo nace pagado.)
     */
    public static Suscripcion crearPeriodoPagado(Negocio negocio, Plan plan, LocalDate fechaInicio,
                                                 LocalDate fechaPago, String metodoPago, String notas) {
        return Suscripcion.builder()
                .negocio(negocio)
                .plan(plan)
                .precio(plan.getPrecio())
                .fechaInicio(fechaInicio)
                .fechaVencimiento(fechaInicio.plusDays(DIAS_PERIODO))
                .fechaPago(fechaPago)
                .metodoPago(metodoPago)
                .estatus("pagada")
                .notas(notas)
                .build();
    }

    public static SuscripcionResponseDTO toResponse(Suscripcion entidad) {
        return SuscripcionResponseDTO.builder()
                .idSuscripcion(entidad.getIdSuscripcion())
                .negocio(mapearNegocio(entidad.getNegocio()))
                .plan(mapearPlan(entidad.getPlan()))
                .precio(entidad.getPrecio())
                .fechaInicio(entidad.getFechaInicio())
                .fechaVencimiento(entidad.getFechaVencimiento())
                .fechaPago(entidad.getFechaPago())
                .metodoPago(entidad.getMetodoPago())
                .estatus(entidad.getEstatus())
                .notas(entidad.getNotas())
                .build();
    }

    private static CatalogoRefDTO mapearNegocio(Negocio negocio) {
        return CatalogoRefDTO.builder()
                .id(negocio.getIdNegocio().intValue())
                .nombre(negocio.getNombre())
                .build();
    }

    private static CatalogoRefDTO mapearPlan(Plan plan) {
        return CatalogoRefDTO.builder()
                .id(plan.getIdPlan().intValue())
                .nombre(plan.getNombre())
                .build();
    }
}