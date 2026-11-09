package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "RegimenFiscalResponseDTO", description = "Régimen fiscal del catálogo del SAT")
public class RegimenFiscalResponseDTO {

    @Schema(example = "612")
    private String clave;

    @Schema(example = "Personas Físicas con Actividades Empresariales y Profesionales")
    private String descripcion;

    @Schema(description = "Aplica a personas físicas (RFC de 13 caracteres)")
    private Boolean personaFisica;

    @Schema(description = "Aplica a personas morales (RFC de 12 caracteres)")
    private Boolean personaMoral;
}
