package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

// Alta de un producto con sus atributos y todas sus variantes en una sola petición
@Getter
@Setter
@Schema(name = "ProductoRequestDTO", description = "Producto con sus variantes")
public class ProductoRequestDTO extends ProductoDatosRequestDTO {

    @Schema(description = "Atributos que generan las variantes (Color, Talla...). Vacío = producto simple")
    private List<UUID> uuidsAtributos;

    @NotEmpty
    @Valid
    @Schema(description = "Variantes a crear. Sin atributos debe haber exactamente una")
    private List<VarianteRequestDTO> variantes;
}