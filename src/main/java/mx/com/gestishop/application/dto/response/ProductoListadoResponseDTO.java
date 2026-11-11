package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

// Fila del listado de productos con su resumen de existencias (variantes activas)
@Getter
@Builder
@Schema(name = "ProductoListadoResponseDTO")
public class ProductoListadoResponseDTO {

    private UUID uuidProducto;
    private String nombre;
    private String marca;
    private String modelo;
    private UUID uuidCategoria;
    private String categoria;
    private String unidad;
    private Short tasaIva;
    private String estatus;
    private int totalVariantes;
    private BigDecimal stockTotal;
    private BigDecimal precioDesde;
    private BigDecimal precioHasta;

    @Schema(description = "Ruta de la miniatura de la foto principal, lista para usarse como src (/api/v1/archivos/ver/...); null si no tiene fotos")
    private String fotoPrincipal;

    @Schema(description = "Variantes activas con stock 0")
    private int variantesSinExistencia;

    @Schema(description = "Variantes activas con stock igual o menor al mínimo (y mayor a 0)")
    private int variantesStockBajo;
}