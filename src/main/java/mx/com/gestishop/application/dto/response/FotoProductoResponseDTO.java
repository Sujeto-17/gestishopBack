package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// Foto de un producto
@Getter
@Builder
@Schema(name = "FotoProductoResponseDTO")
public class FotoProductoResponseDTO {
    private UUID uuidFoto;

    @Schema(description = "Ruta de la foto completa (1200 px), ej. /api/v1/archivos/ver/negocios/1/productos/5/abc.jpg")
    private String rutaImagen;

    @Schema(description = "Ruta de la miniatura (300 px) para listados")
    private String rutaMiniatura;
    private Boolean esPrincipal;
    private Short orden;
}
