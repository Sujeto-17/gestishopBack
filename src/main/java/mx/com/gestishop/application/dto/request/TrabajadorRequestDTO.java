package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import mx.com.gestishop.application.dto.TrabajadorBaseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

// Alta completa: datos del trabajador + acceso al sistema
@Getter
@Setter
@Schema(name = "TrabajadorRequestDTO", description = "Alta completa de un trabajador (acceso + datos laborales)")
public class TrabajadorRequestDTO extends TrabajadorBaseDTO {

    @NotBlank
    @Email
    @Schema(example = "juan.lopez@thejulians.com")
    private String correo;

    /**
     * Normaliza el correo al recibirlo: sin espacios y en minúsculas.
     * Así 'Juan@X.com' y 'juan@x.com' son la misma cuenta (el índice único de BD ignora mayúsculas).
     */
    public void setCorreo(String correo) {
        this.correo = correo == null ? null : correo.trim().toLowerCase();
    }

    @NotBlank
    @Size(min = 8)
    @Schema(description = "Contraseña generada por el sistema, se encripta antes de guardar", example = "Xk9#mPz2")
    private String password;

    @Valid
    @NotNull(message = "Debes elegir el horario inicial del trabajador")
    @Schema(description = "Horario con el que inicia el trabajador (rige desde su fecha de ingreso si no se indica otra)")
    private AsignarHorarioRequestDTO horario;
}