package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "CategoriaSugeridaResponseDTO", description = "Categoría que se ofrece según el giro del negocio")
public class CategoriaSugeridaResponseDTO {

    private Long idSugerida;
    private String nombre;

    @Schema(description = "Nombre de la familia si es una subfamilia; null si es una familia")
    private String nombrePadre;

    @Schema(description = "true si el negocio ya tiene una categoría equivalente")
    private boolean yaAdoptada;
}
