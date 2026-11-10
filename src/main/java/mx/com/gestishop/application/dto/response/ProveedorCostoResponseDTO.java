package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Costo de una variante con un proveedor (solo admin)
@Getter
@Builder
@Schema(name = "ProveedorCostoResponseDTO")
public class ProveedorCostoResponseDTO {
    private UUID uuidProveedor;
    private String proveedor;
    private BigDecimal costo;

    @Schema(description = "true = el costo ya trae IVA; false = el proveedor suma el IVA al facturar")
    private Boolean preciosIncluyenIva;
    private String codigoProveedor;
    private Boolean esPreferido;
    private String notas;
    private LocalDate ultimaCompra;
}

