package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Descuento de un proveedor. El monto es obligatorio salvo en el tipo "fijo", que siempre aplica
@Getter
@Setter
@Schema(name = "ProveedorDescuentoRequestDTO", description = "Descuento de un proveedor")
public class ProveedorDescuentoRequestDTO {

    @NotBlank(message = "El tipo de descuento es obligatorio")
    @Pattern(regexp = "^(mensual|por_compra|fijo)$", message = "El tipo de descuento no es válido")
    @Schema(description = "mensual = compra mensual acumulada; por_compra = monto de una sola compra; fijo = siempre aplica",
            allowableValues = {"mensual", "por_compra", "fijo"}, example = "mensual")
    private String tipo;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Monto a partir del cual aplica. Obligatorio salvo en el tipo fijo", example = "10000", nullable = true)
    private BigDecimal montoMinimo;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100", message = "El porcentaje no puede pasar de 100")
    @Digits(integer = 3, fraction = 2)
    @Schema(example = "5")
    private BigDecimal porcentaje;

    @Size(max = 150)
    @Schema(description = "Aclaraciones, por ejemplo \"10 % en vinil desde 5 rollos\"", nullable = true)
    private String descripcion;

    public void setTipo(String valor) { this.tipo = valor == null || valor.isBlank() ? null : valor.trim(); }

    public void setDescripcion(String valor) {
        this.descripcion = valor == null || valor.isBlank() ? null : valor.trim();
    }
}
