package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Un movimiento de la bitácora de inventario
@Getter
@Builder
@Schema(name = "MovimientoResponseDTO")
public class MovimientoResponseDTO {

    private UUID uuidMovimiento;
    private OffsetDateTime fecha;

    @Schema(allowableValues = {"entrada", "salida", "ajuste"})
    private String tipo;
    private String motivo;

    @Schema(description = "Con signo: positiva suma, negativa resta")
    private BigDecimal cantidad;
    private BigDecimal stockAnterior;
    private BigDecimal stockResultante;

    private UUID uuidVariante;
    private String producto;
    @Schema(description = "Combinación de valores de la variante, p. ej. \"Rojo / M\"")
    private String variante;
    private String unidad;

    @Schema(description = "Quién lo registró")
    private String usuario;
    @Schema(description = "Proveedor de la compra (solo resurtidos)", nullable = true)
    private String proveedor;
    private String notas;

    @Schema(description = "Privado del admin: el gerente siempre recibe null", nullable = true)
    private BigDecimal costoUnitario;
    @Schema(description = "true = resurtido sin costo que el admin aún debe completar. Solo se informa al admin", nullable = true)
    private Boolean pendienteCosto;
}
