package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

// Datos para crear o actualizar un plan de suscripción
@Getter
@Setter
@Schema(name = "PlanRequestDTO", description = "Datos de un plan de suscripción del catálogo global")
public class PlanRequestDTO {

    @NotBlank
    @Schema(description = "Nivel del plan", example = "pro", allowableValues = {"basico", "pro", "elite"})
    private String nivel;

    @NotBlank
    @Schema(description = "Tipo de sistema al que aplica", example = "servicio", allowableValues = {"servicio", "tienda"})
    private String sistemaType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Schema(description = "Precio mensual", example = "599.00")
    private BigDecimal precio;

    @NotBlank
    @Size(max = 160)
    @Schema(description = "Descripción visible para el negocio", example = "Flujo completo de cotización y producción")
    private String descripcion;

    @NotEmpty(message = "Debes seleccionar al menos un módulo")
    @Schema(description = "IDs de los módulos incluidos en este plan", example = "[1,2,3]")
    private List<Long> idModulos;
}
