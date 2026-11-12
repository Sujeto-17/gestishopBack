package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

// Mercancía que llegó de un proveedor: la cantidad se SUMA a la existencia actual
@Getter
@Setter
@Schema(name = "ResurtidoRequestDTO", description = "Entrada de mercancía por compra a un proveedor")
public class ResurtidoRequestDTO {

    @NotNull
    @Schema(description = "Variante que llegó")
    private UUID uuidVariante;

    @NotNull
    @Schema(description = "Proveedor al que se le compró esta vez")
    private UUID uuidProveedor;

    @NotNull
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
    @Digits(integer = 9, fraction = 3)
    @Schema(description = "Cantidad que llegó (se suma a la existencia)", example = "35")
    private BigDecimal cantidad;

    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Costo unitario de esta compra. Solo lo puede capturar el admin; el gerente lo deja vacío y el admin lo completa después")
    private BigDecimal costoUnitario;

    @Size(max = 300)
    private String notas;

    // Los textos vacíos se guardan como null
    public void setNotas(String notas) {
        this.notas = notas == null || notas.isBlank() ? null : notas.trim();
    }
}