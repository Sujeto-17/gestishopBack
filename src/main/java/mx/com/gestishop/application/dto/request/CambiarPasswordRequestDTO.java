package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Datos para que un usuario autenticado cambie su propia contraseña
@Getter
@Setter
@Schema(name = "CambiarPasswordRequestDTO", description = "Datos para el cambio de contraseña")
public class CambiarPasswordRequestDTO {

    @NotNull(message = "El identificador del usuario es obligatorio")
    @Schema(description = "Identificador público del usuario que cambia su contraseña")
    private UUID uuidUsuario;

    @NotBlank(message = "Debes indicar tu contraseña actual")
    @Schema(description = "Contraseña actual, se valida antes de permitir el cambio")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
    @Schema(description = "Nueva contraseña, se encripta con BCrypt antes de guardar", example = "NuevoPassword123!")
    private String passwordNueva;
}
