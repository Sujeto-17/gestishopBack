package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import mx.com.gestishop.core.dto.CatalogoRefDTO;

import java.time.OffsetDateTime;
import java.util.UUID;

// Representación de un administrador, incluyendo el negocio al que está asignado
@Getter
@Builder
@Schema(name = "AdminResponseDTO", description = "Administrador con su negocio asignado")
public class AdminResponseDTO {

    @Schema(description = "Identificador público del usuario")
    private UUID uuidUsuario;

    @Schema(example = "Julian López")
    private String nombre;

    @Schema(example = "julian@thejulians.com")
    private String correo;

    @Schema(example = "9931000001", nullable = true)
    private String telefono;

    @Schema(description = "Negocio al que está asignado este administrador")
    private CatalogoRefDTO negocio;

    @Schema(description = "Estatus de la cuenta", example = "activo")
    private String estatus;

    @Schema(description = "Último acceso registrado, null si nunca ha entrado", nullable = true)
    private OffsetDateTime ultimoAcceso;

    @Schema(description = "Fecha en que se dio de alta")
    private java.time.LocalDate fechaAlta;
}