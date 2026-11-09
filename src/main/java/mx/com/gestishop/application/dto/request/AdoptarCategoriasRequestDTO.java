package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Categorías sugeridas que el admin decide copiar a las de su negocio
@Getter
@Setter
@Schema(name = "AdoptarCategoriasRequestDTO")
public class AdoptarCategoriasRequestDTO {

    @NotEmpty(message = "Selecciona al menos una categoría")
    @Size(max = 100)
    @Schema(description = "Identificadores de las categorías sugeridas elegidas")
    private List<Long> idsSugeridas;
}

