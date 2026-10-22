package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Datos para asignar un administrador que ya existe a otro negocio (dueño con varias sucursales)
@Getter
@Setter
@Schema(name = "VincularNegocioRequestDTO", description = "Negocio al que se asignará el administrador")
public class VincularNegocioRequestDTO {

    @NotNull(message = "El negocio es obligatorio")
    @Schema(description = "ID del negocio")
    private Long idNegocio;
}
