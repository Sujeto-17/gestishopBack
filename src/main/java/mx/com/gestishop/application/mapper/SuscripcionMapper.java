package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.model.Suscripcion;

import java.time.LocalDate;

/**
 * Convierte entre la entidad Suscripcion y sus DTOs. Incluye la fábrica
 * para crear el PRIMER periodo de un negocio nuevo (periodo de prueba),
 * usada desde NegocioServiceImpl al dar de alta un negocio.
 */
public class SuscripcionMapper {

    private static final int DIAS_PRUEBA = 15;

    private SuscripcionMapper() {
    }

    /**
     * Construye la primera suscripción de un negocio recién creado, en
     * estatus 'pendiente' (aún no se ha cobrado, es periodo de prueba).
     * Las fechas coinciden exactamente con negocio.fechaAlta/vencimiento,
     * calculadas en NegocioMapper, para no duplicar esa lógica en dos lugares.
     */
    public static Suscripcion crearPeriodoPrueba(Negocio negocio, Plan plan) {
        return Suscripcion.builder()
                .negocio(negocio)
                .plan(plan)
                .precio(plan.getPrecio())
                .fechaInicio(negocio.getFechaAlta())
                .fechaVencimiento(negocio.getVencimiento())
                .estatus("pendiente")
                .notas("Periodo de prueba inicial de " + DIAS_PRUEBA + " días")
                .build();
    }

    /**
     * Construye el siguiente periodo al renovar: 30 días de vigencia a partir
     * de hoy (o desde el vencimiento anterior si aún no vence, para no perder días).
     */
    public static Suscripcion crearRenovacion(Negocio negocio, Plan plan, LocalDate fechaInicio) {
        return Suscripcion.builder()
                .negocio(negocio)
                .plan(plan)
                .precio(plan.getPrecio())
                .fechaInicio(fechaInicio)
                .fechaVencimiento(fechaInicio.plusDays(30))
                .estatus("pendiente")
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