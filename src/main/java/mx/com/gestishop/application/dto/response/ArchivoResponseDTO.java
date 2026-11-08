package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(name = "ArchivoResponseDTO")
public class ArchivoResponseDTO {

    private UUID uuidArchivo;
    private String nombreOriginal;
    private String contentType;
    @Schema(description = "Ruta relativa usada para construir la URL de descarga/visualización")
    private String rutaRelativa;
    @Schema(description = "Ruta relativa de la miniatura (300 px) para listados. Las imágenes anteriores a esta versión no la tienen.")
    private String rutaMiniatura;
    private Boolean esPrincipal;
}
