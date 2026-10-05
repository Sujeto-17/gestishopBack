package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.dto.StoredImageDTO;
import mx.com.gestishop.core.interfaces.FileStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sirve archivos físicos (imágenes) directamente como bytes, dado su ruta
 * relativa guardada en la tabla archivos. Es un endpoint PÚBLICO (sin JWT)
 * porque un <img src="..."> del navegador no puede mandar headers de
 * autorización — la "seguridad" aquí es que la ruta no es adivinable
 * (usa nombres generados con UUID), no autenticación por token.
 */
@Tag(name = "Archivos", description = "Servir imágenes y documentos ya guardados")
@RestController
@RequestMapping("/api/v1/archivos")
@RequiredArgsConstructor
public class ArchivoViewController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Obtener una imagen por su ruta relativa",
            description = "Devuelve los bytes de la imagen con el Content-Type correcto, lista para un <img src>.")
    @GetMapping("/ver/**")
    public ResponseEntity<byte[]> ver(HttpServletRequest request) {
        // Se extrae todo lo que viene después de /archivos/ver/ como ruta relativa,
        // ej. /archivos/ver/negocios/14/a1b2c3d4.jpg → "negocios/14/a1b2c3d4.jpg"
        String rutaRelativa = request.getRequestURI().split("/archivos/ver/")[1];

        StoredImageDTO imagen = fileStorageService.getImage(rutaRelativa);

        MediaType mediaType = imagen.getContentType() != null
                ? MediaType.parseMediaType(imagen.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7))) // el navegador cachea, menos carga al server
                .body(imagen.getBytes());
    }
}
