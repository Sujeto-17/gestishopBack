package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Una variante con su precio de venta, sus proveedores y la ganancia calculada con el proveedor preferido (solo admin)
@Getter
@Builder
@Schema(name = "VarianteCostosResponseDTO")
public class VarianteCostosResponseDTO {
    private UUID uuidVariante;

    @Schema(description = "Texto para mostrar, ej. \"Rojo / M\"; vacío en un producto simple")
    private String etiqueta;
    private String estatus;
    private BigDecimal precioPublico;
    private BigDecimal precioMayoreo;

    @Schema(description = "Costo del proveedor preferido; null si aún no hay ninguno")
    private BigDecimal costoPreferido;

    @Schema(description = "Precio al público menos costo preferido")
    private BigDecimal gananciaMonto;

    @Schema(description = "Ganancia como porcentaje del precio al público")
    private BigDecimal margenPorcentaje;
    private List<ProveedorCostoResponseDTO> proveedores;
}
