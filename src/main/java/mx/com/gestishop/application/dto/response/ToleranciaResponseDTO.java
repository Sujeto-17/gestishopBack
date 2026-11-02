package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

// Una opción del catálogo de tolerancias
@Getter
@Builder
@Schema(name = "ToleranciaResponseDTO", description = "Minutos de tolerancia de retardo")
public class ToleranciaResponseDTO {

    private Long idTolerancia;

    @Schema(example = "15")
    private Integer minutos;

    @Schema(example = "15 minutos")
    private String nombre;
}
