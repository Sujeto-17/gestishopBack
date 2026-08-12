package mx.com.gestishop.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Clase genérica para respuestas de la API que incluyen datos específicos.
 * Esta clase extiende ApiBaseResponseDTO para incluir las propiedades base
 * de la respuesta, y agrega una propiedad genérica "datos" para contener
 * los datos específicos de cada respuesta.
 *
 * @param <T> El tipo de datos que se incluirá en la respuesta.
 */
@Getter
@SuperBuilder
@Schema(name = "ApiDataResponseDTO",
        description = "Estructura estándar con datos para la respuesta de la API")
public class ApiDataResponseDTO<T> extends ApiBaseResponseDTO {

    @Schema(description = "Datos retornados por la operación")
    private final T datos;

}
