package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos para crear o editar un valor de atributo (del sistema o propio de un negocio)
@Getter
@Setter
@Schema(name = "AtributoValorRequestDTO", description = "Valor de un atributo")
public class AtributoValorRequestDTO {

    @NotBlank
    @Size(max = 40)
    @Schema(example = "Turquesa")
    private String valor;

    @Min(0)
    @Max(999)
    @Schema(description = "Posición al listar; opcional", example = "0")
    private Short orden;
}
