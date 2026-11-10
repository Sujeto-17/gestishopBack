package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Costo de una variante con un proveedor: si ya existía se actualiza, si no se agrega
@Getter
@Setter
@Schema(name = "VarianteProveedorRequestDTO", description = "Costo de una variante con un proveedor")
public class VarianteProveedorRequestDTO {

    @NotNull
    @Schema(description = "Proveedor del negocio que surte la variante")
    private UUID uuidProveedor;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Costo tal como lo cotiza el proveedor", example = "62.00")
    private BigDecimal costo;

    @Size(max = 40)
    @Schema(description = "Clave que el proveedor usa para este artículo")
    private String codigoProveedor;

    @Size(max = 200)
    private String notas;

    @Schema(description = "Fecha de la última compra a este proveedor")
    private LocalDate ultimaCompra;

    @Schema(description = "true = pasa a ser el proveedor preferido. El primero que se registra lo es siempre")
    private Boolean esPreferido;

    // Los textos vacíos se guardan como null
    public void setCodigoProveedor(String codigoProveedor) {
        this.codigoProveedor = codigoProveedor == null || codigoProveedor.isBlank() ? null : codigoProveedor.trim();
    }

    public void setNotas(String notas) {
        this.notas = notas == null || notas.isBlank() ? null : notas.trim();
    }
}