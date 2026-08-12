package mx.com.gestishop.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * DTO para representar los metadatos de paginación en las respuestas de la API.
 * Esta clase se utiliza para proporcionar información sobre la paginación de
 * los resultados, como el número de página actual, el tamaño de página, el total
 * de elementos, etc.
 */
@Getter
@Builder
@Schema(name = "PageMetadataDTO",
        description = "DTO que representa los metadatos de paginación en las respuestas de la API.")
public class PageMetadataDTO {

    @Schema(description = "Número de página actual (base 0)", example = "0")
    private final int number;

    @Schema(description = "Cantidad de elementos por página", example = "10")
    private final int size;

    @Schema(description = "Total de elementos", example = "57")
    private final long totalElements;

    @Schema(description = "Total de páginas", example = "6")
    private final int totalPages;

    @Schema(description = "Indica si es la primera página", example = "true")
    private final boolean first;

    @Schema(description = "Indica si es la última página", example = "false")
    private final boolean last;

    @Schema(description = "Orden aplicado", example = "nombre,asc")
    private final String sort;

    /**
     * Metodo estático para crear una instancia de PageMetadataDTO a partir de
     * un objeto Page.
     *
     * @param page El objeto Page del cual se extraerán los metadatos de
     *             paginación.
     * @return Una instancia de PageMetadataDTO con los metadatos extraídos del
     * objeto Page.
     */
    public static PageMetadataDTO from(Page<?> page) {
        return PageMetadataDTO.builder()
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

}
