package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para que un administrador con varios negocios cambie el negocio con el que está trabajando.
 * Se envía también el refresh token actual: se rota y queda ligado al nuevo negocio, para que el
 * cambio no se pierda cuando el access token se renueve a los 15 minutos.
 */
@Getter
@Setter
@Schema(name = "CambiarNegocioRequestDTO", description = "Datos para cambiar de negocio activo")
public class CambiarNegocioRequestDTO {

    @NotNull(message = "El negocio es obligatorio")
    @Schema(description = "ID del negocio al que se quiere entrar")
    private Long idNegocio;

    @NotBlank(message = "El refresh token es obligatorio")
    @Schema(description = "Refresh token de la sesión actual")
    private String refreshToken;
}
