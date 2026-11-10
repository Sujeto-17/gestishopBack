package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos para crear o editar un atributo global (superadmin)
@Getter
@Setter
@Schema(name = "AtributoRequestDTO", description = "Atributo global de variante")
public class AtributoRequestDTO {

    @NotBlank
    @Size(max = 40)
    @Schema(example = "Color")
    private String nombre;

    @Min(0)
    @Max(999)
    @Schema(description = "Posición al listar; opcional", example = "0")
    private Short orden;
}
