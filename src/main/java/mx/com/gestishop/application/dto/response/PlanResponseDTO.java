package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Representación completa de un plan, incluyendo sus módulos incluidos
@Getter
@Builder
@Schema(name = "PlanResponseDTO", description = "Plan de suscripción con sus módulos incluidos")
public class PlanResponseDTO {

    @Schema(description = "Identificador interno")
    private Long idPlan;

    @Schema(description = "Identificador público")
    private UUID uuidPlan;

    @Schema(description = "Nivel del plan", example = "pro")
    private String nivel;

    @Schema(description = "Nombre visible, derivado del nivel", example = "Pro")
    private String nombre;

    @Schema(description = "Tipo de sistema", example = "servicio")
    private String sistemaType;

    @Schema(description = "Precio mensual")
    private BigDecimal precio;

    @Schema(description = "Descripción del plan")
    private String descripcion;

    @Schema(description = "Estatus del plan", example = "activo")
    private String estatus;

    @Schema(description = "Módulos incluidos en este plan")
    private List<ModuloResponseDTO> modulos;
}
