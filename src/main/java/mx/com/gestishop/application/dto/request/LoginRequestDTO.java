package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos que el cliente envía para iniciar sesión.
 * Aplica por igual a superadmin, admin y trabajador — el backend
 * determina el tipo de usuario internamente según lo que encuentre en BD.
 */
@Getter
@Setter
@Schema(name = "LoginRequestDTO", description = "Credenciales de acceso al sistema")
public class LoginRequestDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Schema(description = "Correo electrónico registrado", example = "julian@thejulians.com")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Schema(description = "Contraseña en texto plano (se valida contra el hash almacenado)", example = "MiPassword123!")
    private String password;
}
