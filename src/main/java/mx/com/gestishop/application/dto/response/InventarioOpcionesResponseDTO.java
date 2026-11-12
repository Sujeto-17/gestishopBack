package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Lo necesario para llenar los selectores al registrar movimientos de inventario (sin ningún costo)
@Getter
@Builder
@Schema(name = "InventarioOpcionesResponseDTO")
public class InventarioOpcionesResponseDTO {

    private List<VarianteOpcion> variantes;
    private List<ProveedorOpcion> proveedores;

    @Getter
    @Builder
    public static class VarianteOpcion {
        private UUID uuidVariante;
        private String producto;
        @Schema(description = "Combinación de valores, p. ej. \"Rojo\"; vacío en un producto simple")
        private String variante;
        private BigDecimal existencia;
        private String unidad;
    }

    @Getter
    @Builder
    public static class ProveedorOpcion {
        private UUID uuidProveedor;
        private String nombre;
    }
}

