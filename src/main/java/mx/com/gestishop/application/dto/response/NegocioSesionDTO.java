package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Un negocio al que un administrador puede entrar. Llega en el login (y al cambiar de negocio)
 * para que el front arme el selector de negocio del dueño que tiene varios negocios o sucursales.
 */
@Getter
@Builder
@Schema(name = "NegocioSesionDTO", description = "Negocio disponible para un administrador (selector de negocio)")
public class NegocioSesionDTO {

    @Schema(description = "Identificador del negocio")
    private Long idNegocio;

    @Schema(description = "Nombre del negocio", example = "Imprenta López - Centro")
    private String nombre;

    @Schema(description = "URL pública del logo, null si no tiene", nullable = true)
    private String logoUrl;

    @Schema(description = "false si el negocio no se puede usar ahora (periodo vencido o suspendido)")
    private Boolean disponible;

    @Schema(description = "Motivo por el que no está disponible; null si sí lo está", nullable = true)
    private String motivo;
}