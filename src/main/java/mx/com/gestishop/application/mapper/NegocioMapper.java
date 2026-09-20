package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.NegocioRequestDTO;
import mx.com.gestishop.application.dto.response.NegocioResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.CatGiroNegocio;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;

import java.time.LocalDate;

/**
 * Convierte entre la entidad Negocio y sus DTOs. La fecha de vencimiento
 * NUNCA se captura manualmente en el DTO de entrada — todo negocio nuevo
 * entra en periodo de prueba de 15 días, calculado aquí, para evitar que
 * alguien capture una fecha arbitraria que rompa la lógica de vencimientos.
 */
public class NegocioMapper {

    private static final int DIAS_PRUEBA = 15;

    private NegocioMapper() {
    }

    // Construye la entidad; giro y plan se asignan aparte en el service (requieren consulta a BD)
    public static Negocio toEntity(NegocioRequestDTO dto) {
        return Negocio.builder()
                .nombre(dto.getNombre())
                .logoUrl(dto.getLogoUrl())
                .sistemaType(dto.getSistemaType())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .direccion(dto.getDireccion())
                .estatus("prueba")  // todo negocio nuevo inicia en periodo de prueba
                .fechaAlta(LocalDate.now())
                .vencimiento(LocalDate.now().plusDays(DIAS_PRUEBA))
                .build();
    }

    public static NegocioResponseDTO toResponse(Negocio entidad) {
        return NegocioResponseDTO.builder()
                .idNegocio(entidad.getIdNegocio())
                .uuidNegocio(entidad.getUuidNegocio())
                .nombre(entidad.getNombre())
                .logoUrl(entidad.getLogoUrl())
                .sistemaType(entidad.getSistemaType())
                .giro(mapearGiro(entidad.getGiro()))
                .correo(entidad.getCorreo())
                .telefono(entidad.getTelefono())
                .direccion(entidad.getDireccion())
                .plan(mapearPlan(entidad.getPlan()))
                .estatus(entidad.getEstatus())
                .fechaAlta(entidad.getFechaAlta())
                .vencimiento(entidad.getVencimiento())
                .build();
    }

    private static CatalogoRefDTO mapearGiro(CatGiroNegocio giro) {
        return CatalogoRefDTO.builder()
                .id(giro.getIdGiro().intValue())
                .nombre(giro.getNombre())
                .build();
    }

    private static CatalogoRefDTO mapearPlan(Plan plan) {
        return CatalogoRefDTO.builder()
                .id(plan.getIdPlan().intValue())
                .nombre(plan.getNombre())
                .build();
    }
}
