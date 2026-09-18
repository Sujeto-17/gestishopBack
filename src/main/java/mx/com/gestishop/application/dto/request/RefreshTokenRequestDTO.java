package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Cuerpo usado tanto para renovar sesión (/refresh) como para cerrarla (/logout)
@Getter
@Setter
@Schema(name = "RefreshTokenRequestDTO", description = "Refresh token de la sesión a renovar o cerrar")
public class RefreshTokenRequestDTO {

    @NotBlank(message = "El refresh token es obligatorio")
    @Schema(description = "Token opaco recibido en el login, guardado por el cliente")
    private String refreshToken;
}
