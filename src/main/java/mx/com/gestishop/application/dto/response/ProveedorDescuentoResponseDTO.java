package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(name = "ProveedorDescuentoResponseDTO")
public class ProveedorDescuentoResponseDTO {

    @Schema(description = "mensual | por_compra | fijo")
    private String tipo;
    @Schema(description = "null en el tipo fijo")
    private BigDecimal montoMinimo;
    private BigDecimal porcentaje;
    private String descripcion;
}


