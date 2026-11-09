package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "ConfiguracionNegocioResponseDTO", description = "Ajustes del negocio en el que se trabaja")
public class ConfiguracionNegocioResponseDTO {
    private String nombreNegocio;
    private Boolean emiteFactura;
    private Boolean preciosIncluyenIva;
}
