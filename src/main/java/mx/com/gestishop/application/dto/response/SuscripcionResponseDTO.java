package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import mx.com.gestishop.core.dto.CatalogoRefDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

// Representación de un periodo de suscripción (una fila del historial de pagos de un negocio)
@Getter
@Builder
@Schema(name = "SuscripcionResponseDTO", description = "Un periodo de suscripción con su estatus de pago")
public class SuscripcionResponseDTO {

    @Schema(description = "Identificador del periodo de suscripción")
    private Long idSuscripcion;

    @Schema(description = "Negocio al que pertenece este periodo")
    private CatalogoRefDTO negocio;

    @Schema(description = "Plan vigente en este periodo")
    private CatalogoRefDTO plan;

    @Schema(description = "Precio pagado en este periodo específico (histórico, no cambia si el plan sube de precio después)")
    private BigDecimal precio;

    @Schema(description = "Inicio del periodo")
    private LocalDate fechaInicio;

    @Schema(description = "Vencimiento del periodo")
    private LocalDate fechaVencimiento;

    @Schema(description = "Fecha en que se registró el pago, null si sigue pendiente", nullable = true)
    private LocalDate fechaPago;

    @Schema(description = "Método de pago utilizado", nullable = true)
    private String metodoPago;

    @Schema(description = "Estatus del periodo", example = "pagada",
            allowableValues = {"pagada", "pendiente", "vencida", "cancelada"})
    private String estatus;

    @Schema(nullable = true)
    private String notas;
}
