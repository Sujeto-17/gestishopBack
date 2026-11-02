package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Datos para crear o editar un horario propio del negocio
@Getter
@Setter
@Schema(name = "HorarioRequestDTO", description = "Horario de trabajo: nombre y los días que se trabajan")
public class HorarioRequestDTO {

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Nombre visible del horario", example = "Sábados medios")
    private String nombre;

    @Valid
    @NotEmpty(message = "Debes agregar al menos un día de trabajo")
    @Size(max = 7, message = "Un horario tiene como máximo 7 días")
    @Schema(description = "Días que se trabajan; un día que no aparece es día de descanso")
    private List<HorarioDiaRequestDTO> dias;
}
