package mx.com.gestishop.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Clase genérica para respuestas de la API que incluyen datos paginados.
 * Esta clase extiende ApiBaseResponseDTO para incluir las propiedades base
 * de la respuesta, y agrega una propiedad genérica "datos" para contener los
 * datos específicos de cada respuesta, así como una propiedad "page" para
 * contener los metadatos de paginación.
 *
 * @param <T> El tipo de datos que se incluirá en la respuesta.
 */
@Getter
@SuperBuilder
@Schema(name = "ApiPageResponseDTO",
        description = "DTO genérico para respuestas de API que incluyen datos paginados y metadatos de paginación.")
public class ApiPageResponseDTO<T> extends ApiBaseResponseDTO {

    @Schema(description = "Datos retornados por la operación")
    private final List<T> datos;

    @Schema(description = "Información de paginación (solo cuando aplica)")
    private final PageMetadataDTO page;

}
