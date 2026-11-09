package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "UnidadMedidaResponseDTO")
public class UnidadMedidaResponseDTO {

    private Long idUnidad;

    @Schema(example = "kg")
    private String clave;

    @Schema(example = "Kilogramo")
    private String nombre;

    @Schema(description = "true si la cantidad puede llevar decimales (metros, litros, kilos)")
    private Boolean permiteDecimales;
}

