package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Datos para registrar el pago de un periodo de suscripción ya existente.
 * No se usa para CREAR la suscripción (eso lo hace el sistema automáticamente
 * al dar de alta un negocio o al renovar), solo para confirmar que se pagó.
 */
@Getter
@Setter
@Schema(name = "SuscripcionRequestDTO", description = "Datos para confirmar el pago de un periodo de suscripción")
public class SuscripcionRequestDTO {

    @NotNull
    @Schema(description = "Fecha en que se recibió el pago", example = "2026-08-01")
    private LocalDate fechaPago;

    @NotBlank
    @Schema(description = "Método de pago utilizado", example = "transferencia",
            allowableValues = {"transferencia", "tarjeta", "efectivo", "otro"})
    private String metodoPago;

    @Schema(description = "Notas adicionales sobre el pago", nullable = true)
    private String notas;
}
