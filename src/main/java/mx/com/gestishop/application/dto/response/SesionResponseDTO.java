package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Representa la identidad básica de un usuario, usado como respuesta
 * al registrar una cuenta nueva (antes de que haga login por primera vez).
 */
@Getter
@Builder
@Schema(name = "SesionResponseDTO", description = "Identidad básica de un usuario registrado")
public class SesionResponseDTO {

    @Schema(description = "Identificador público del usuario")
    private UUID uuidUsuario;

    @Schema(description = "Nombre completo")
    private String nombre;

    @Schema(description = "Correo registrado")
    private String correo;

    @Schema(description = "Tipo de usuario creado", example = "admin")
    private String tipoUsuario;
}
