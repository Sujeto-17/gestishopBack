package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Variante con sus precios y existencia. No incluye costos ni proveedores (eso es solo del admin)
@Getter
@Builder
@Schema(name = "VarianteResponseDTO")
public class VarianteResponseDTO {
    private UUID uuidVariante;

    @Schema(description = "Texto para mostrar, ej. \"Rojo / M\"; vacío en un producto simple")
    private String etiqueta;
    private List<VarianteValorResponseDTO> valores;
    private String sku;
    private String codigoBarras;
    private BigDecimal precioPublico;
    private BigDecimal precioMayoreo;
    private BigDecimal cantidadMayoreo;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private String estatus;
}