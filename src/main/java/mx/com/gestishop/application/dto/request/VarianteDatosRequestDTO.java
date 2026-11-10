package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Datos editables de una variante: identificadores, precios y stock mínimo (el stock real solo cambia con movimientos)
@Getter
@Setter
@Schema(name = "VarianteDatosRequestDTO", description = "Precios y datos de una variante")
public class VarianteDatosRequestDTO {

    @Size(max = 40)
    private String sku;

    @Size(max = 30)
    private String codigoBarras;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Precio de menudeo (precio al público)", example = "30.00")
    private BigDecimal precioPublico;

    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Precio de mayoreo; solo negocios tipo tienda. Va junto con cantidadMayoreo")
    private BigDecimal precioMayoreo;

    @DecimalMin(value = "0.001")
    @Digits(integer = 9, fraction = 3)
    @Schema(description = "Desde cuántas piezas aplica el precio de mayoreo")
    private BigDecimal cantidadMayoreo;

    @DecimalMin("0.000")
    @Digits(integer = 9, fraction = 3)
    @Schema(description = "Stock mínimo para avisar; vacío = 0")
    private BigDecimal stockMinimo;

    // Los textos vacíos se guardan como null
    public void setSku(String sku) {
        this.sku = sku == null || sku.isBlank() ? null : sku.trim();
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras == null || codigoBarras.isBlank() ? null : codigoBarras.trim();
    }
}
