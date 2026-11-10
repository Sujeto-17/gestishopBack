package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Un cambio de costo de una variante con un proveedor (solo admin)
@Getter
@Builder
@Schema(name = "HistorialCostoResponseDTO")
public class HistorialCostoResponseDTO {

    private OffsetDateTime fecha;
    private UUID uuidProveedor;
    private String proveedor;

    @Schema(description = "null cuando es el primer costo registrado")
    private BigDecimal costoAnterior;
    private BigDecimal costoNuevo;
}
