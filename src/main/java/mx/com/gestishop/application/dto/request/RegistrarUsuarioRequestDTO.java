package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para registrar un nuevo usuario en el sistema.
 * Un mismo DTO sirve para dar de alta superadmin, admin o trabajador —
 * el campo tipoUsuario decide el flujo, y idNegocio solo aplica a
 * admin/trabajador (se valida en el service, no aquí).
 */
@Getter
@Setter
@Schema(name = "RegistrarUsuarioRequestDTO", description = "Datos para dar de alta un nuevo usuario del sistema")
public class RegistrarUsuarioRequestDTO {

    @NotBlank
    @Schema(description = "Tipo de usuario a crear", example = "admin", allowableValues = {"superadmin", "admin", "trabajador"})
    private String tipoUsuario;

    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(description = "Nombre completo", example = "Julian López")
    private String nombre;

    @NotBlank
    @Email
    @Schema(description = "Correo electrónico, será el usuario de acceso", example = "julian@thejulians.com")
    private String correo;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(description = "Teléfono a 10 dígitos", example = "9931000001", nullable = true)
    private String telefono;

    /**
     * Contraseña generada por el sistema (o capturada por el superadmin al crear la cuenta).
     * Se recibe en texto plano SOLO en este DTO de entrada; nunca se persiste así
     * (AuthServiceImpl la encripta con BCrypt antes de guardar).
     */
    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Schema(description = "Contraseña inicial en texto plano, se encripta antes de guardar", example = "Xk9#mPz2")
    private String password;

    @Schema(description = "ID del negocio al que pertenece (obligatorio si tipoUsuario es admin o trabajador)", nullable = true)
    private Long idNegocio;
}
