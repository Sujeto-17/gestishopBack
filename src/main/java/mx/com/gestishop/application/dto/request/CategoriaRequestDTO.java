package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Datos para crear o editar una categoría del negocio
@Getter
@Setter
@Schema(name = "CategoriaRequestDTO", description = "Familia o subfamilia de productos")
public class CategoriaRequestDTO {

    @NotBlank
    @Size(max = 80)
    @Schema(example = "Sublimación")
    private String nombre;

    @Size(max = 200)
    private String descripcion;

    @Schema(description = "Familia a la que pertenece. Vacío = es una familia (nivel principal)")
    private UUID uuidPadre;

    @Min(0)
    @Max(999)
    @Schema(description = "Posición al listar; opcional", example = "0")
    private Short orden;
}