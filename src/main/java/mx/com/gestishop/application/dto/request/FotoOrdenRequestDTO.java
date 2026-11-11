package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

// Nuevo orden de las fotos de un producto
@Getter
@Setter
@Schema(name = "FotoOrdenRequestDTO")
public class FotoOrdenRequestDTO {

    @NotEmpty
    @Schema(description = "Todas las fotos del producto, en el orden deseado. La principal se muestra siempre primero")
    private List<UUID> uuidsFotos;
}

