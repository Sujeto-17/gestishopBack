package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.dto.StoredImageDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.interfaces.FileStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sirve imágenes ya guardadas, dada su ruta relativa. Endpoint PÚBLICO (sin JWT) porque un
 * &lt;img src&gt; del navegador no puede mandar cabeceras de autorización; la protección es que
 * las rutas incluyen nombres UUID no adivinables.
 * <p>
 * Endurecimiento: solo se sirven extensiones de imagen conocidas, el Content-Type sale de una tabla fija
 * (nunca del sistema de archivos) y se envían cabeceras que impiden que el navegador lo interprete como
 * otra cosa (nosniff) o ejecute contenido (CSP sandbox).
 */
@Tag(name = "Archivos", description = "Servir imágenes ya guardadas")
@RestController
@RequestMapping("/api/v1/archivos")
@RequiredArgsConstructor
public class ArchivoViewController {

    // Extensión -> tipo de contenido. Incluye formatos antiguos (logos subidos antes del endurecimiento).
    private static final Map<String, String> TIPOS = Map.of(
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "webp", "image/webp");

    private final FileStorageService fileStorageService;

    @Operation(summary = "Obtener una imagen por su ruta relativa",
            description = "Devuelve los bytes de la imagen con el Content-Type correcto, lista para un <img src>.")
    @GetMapping("/ver/**")
    public ResponseEntity<byte[]> ver(HttpServletRequest request) {
        // Todo lo que viene después de /archivos/ver/ es la ruta relativa,
        // ej. /archivos/ver/negocios/14/productos/230/abc.jpg
        String uri = request.getRequestURI();
        int idx = uri.indexOf("/archivos/ver/");
        String rutaRelativa = idx >= 0 ? uri.substring(idx + "/archivos/ver/".length()) : "";

        String tipo = tipoPermitido(rutaRelativa);

        StoredImageDTO imagen = fileStorageService.getImage(rutaRelativa);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7))) // el navegador cachea, menos carga al server
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(imagen.getBytes());
    }

    // Devuelve el tipo fijo según la extensión o rechaza la petición si no es una imagen permitida
    private String tipoPermitido(String ruta) {
        int punto = ruta.lastIndexOf('.');
        String extension = punto >= 0 ? ruta.substring(punto + 1).toLowerCase() : "";
        String tipo = TIPOS.get(extension);
        if (tipo == null || ruta.contains("..") || ruta.contains("\\")) {
            throw new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND, "Archivo no encontrado");
        }
        return tipo;
    }
}
