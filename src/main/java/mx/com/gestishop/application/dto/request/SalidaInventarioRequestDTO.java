package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

// Baja de existencia que no es una venta: merma (se dañó, se perdió) o consumo interno
@Getter
@Setter
@Schema(name = "SalidaInventarioRequestDTO", description = "Merma o consumo interno")
public class SalidaInventarioRequestDTO {

    @NotNull
    private UUID uuidVariante;

    @NotNull
    @Pattern(regexp = "^(merma|consumo)$", message = "El motivo debe ser 'merma' o 'consumo'")
    @Schema(allowableValues = {"merma", "consumo"})
    private String motivo;

    @NotNull
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
    @Digits(integer = 9, fraction = 3)
    @Schema(description = "Cantidad que sale (positiva; el sistema la resta)", example = "2")
    private BigDecimal cantidad;

    @NotBlank(message = "Explica qué pasó")
    @Size(max = 300)
    private String notas;

    public void setNotas(String notas) {
        this.notas = notas == null ? null : notas.trim();
    }
}
