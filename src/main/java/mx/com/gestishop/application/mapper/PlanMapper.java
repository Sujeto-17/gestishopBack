package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.PlanRequestDTO;
import mx.com.gestishop.application.dto.response.PlanResponseDTO;
import mx.com.gestishop.domain.model.CatModulo;
import mx.com.gestishop.domain.model.Plan;

import java.util.Set;

/**
 * Convierte entre la entidad Plan y sus DTOs. El nombre visible ("Básico",
 * "Pro", "Elite") se DERIVA del nivel, no se captura aparte — evita
 * inconsistencias como un plan "elite" llamado "Básico" por error de captura.
 */
public class PlanMapper {

    private PlanMapper() {
    }

    /** Traduce el nivel técnico a su etiqueta visible en español. */
    private static String nombreDesdeNivel(String nivel) {
        return switch (nivel) {
            case "basico" -> "Básico";
            case "pro" -> "Pro";
            case "elite" -> "Elite";
            default -> nivel;
        };
    }

    /** Construye la entidad base; los módulos (relación N:M) se asignan aparte en el service. */
    public static Plan toEntity(PlanRequestDTO dto) {
        return Plan.builder()
                .nivel(dto.getNivel())
                .nombre(nombreDesdeNivel(dto.getNivel()))
                .sistemaType(dto.getSistemaType())
                .precio(dto.getPrecio())
                .descripcion(dto.getDescripcion())
                .estatus("activo")
                .build();
    }

    public static void actualizarEntidad(Plan entidad, PlanRequestDTO dto) {
        entidad.setNivel(dto.getNivel());
        entidad.setNombre(nombreDesdeNivel(dto.getNivel()));
        entidad.setSistemaType(dto.getSistemaType());
        entidad.setPrecio(dto.getPrecio());
        entidad.setDescripcion(dto.getDescripcion());
    }

    public static PlanResponseDTO toResponse(Plan entidad) {
        return PlanResponseDTO.builder()
                .idPlan(entidad.getIdPlan())
                .uuidPlan(entidad.getUuidPlan())
                .nivel(entidad.getNivel())
                .nombre(entidad.getNombre())
                .sistemaType(entidad.getSistemaType())
                .precio(entidad.getPrecio())
                .descripcion(entidad.getDescripcion())
                .estatus(entidad.getEstatus())
                .modulos(mapearModulos(entidad.getModulos()))
                .build();
    }

    private static java.util.List<mx.com.gestishop.application.dto.response.ModuloResponseDTO> mapearModulos(Set<CatModulo> modulos) {
        return modulos.stream().map(CatModuloMapper::toResponse).toList();
    }
}
