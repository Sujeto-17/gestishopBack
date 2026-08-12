package mx.com.gestishop.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Clase base para las respuestas de la API, que incluye propiedades comunes a
 * todas las respuestas.
 * Esta clase se puede extender para incluir propiedades específicas de cada
 * tipo de respuesta.
 */
@Getter
@SuperBuilder
@Schema(name = "ApiBaseResponseDTO",
        description = "Propiedades base comunes a todas las respuestas de la API")
public abstract class ApiBaseResponseDTO {

    @Schema(description = "Código interno de respuesta",
            example = "OK")
    private final String codigo;

    @Schema(description = "Mensaje descriptivo del resultado",
            example = "Petición satisfactoria")
    private final String mensaje;

    @Schema(description = "Tipo de respuesta",
            example = "SUCCESS")
    private final String tipo;

}
