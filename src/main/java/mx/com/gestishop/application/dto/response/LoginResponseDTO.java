package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta del login: tokens de sesión + contexto necesario para que
 * el frontend arme la navegación (qué módulos mostrar, a qué negocio pertenece).
 * Equivale a la interfaz "Sesion" ya definida en el frontend Angular.
 */
@Getter
@Builder
@Schema(name = "LoginResponseDTO", description = "Resultado de un login exitoso")
public class LoginResponseDTO {

    @Schema(description = "Token de acceso JWT de corta duración, se envía en cada request como Bearer")
    private String accessToken;

    @Schema(description = "Token opaco de larga duración, usado únicamente para renovar el access token")
    private String refreshToken;

    @Schema(description = "Indica si el usuario debe cambiar su contraseña antes de continuar (cuenta nueva o password reseteado)")
    private Boolean debeActualizarPassword;

    @Schema(description = "Tipo de usuario autenticado", example = "admin", allowableValues = {"superadmin", "admin", "trabajador"})
    private String tipoUsuario;

    @Schema(description = "Nivel de acceso, solo aplica si tipoUsuario=trabajador", example = "gerente", nullable = true)
    private String nivelAcceso;

    @Schema(description = "ID del negocio al que pertenece, no aplica para superadmin", nullable = true)
    private Long idNegocio;

    @Schema(description = "Lista de nombres de módulos que este usuario puede ver en el menú")
    private List<String> modulos;
}
