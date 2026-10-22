package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos para cancelar un periodo de suscripción registrado por error
@Getter
@Setter
@Schema(name = "CancelarPeriodoRequestDTO", description = "Motivo por el que se cancela un periodo pagado")
public class CancelarPeriodoRequestDTO {

    @NotBlank(message = "Indica el motivo de la cancelación")
    @Size(max = 120, message = "El motivo no puede pasar de 120 caracteres")
    @Schema(description = "Por qué se cancela", example = "Pago registrado en el negocio equivocado")
    private String motivo;
}