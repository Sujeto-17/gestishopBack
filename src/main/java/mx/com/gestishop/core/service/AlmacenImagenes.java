package mx.com.gestishop.core.service;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.dto.StoredFileDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.utils.GeneralUtil;
import mx.com.gestishop.infrastructure.config.StorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Guarda en disco las imágenes ya validadas por {@link ImagenSeguraProcesador}.
 * El nombre del archivo lo genera siempre el servidor (UUID); nada de lo que envía el usuario forma parte de la ruta.
 * Por cada imagen se escriben dos archivos: {uuid}.jpg (principal) y {uuid}_m.jpg (miniatura para listados).
 */
@Component
@RequiredArgsConstructor
public class AlmacenImagenes {

    // Sufijo que distingue a la miniatura de la imagen principal
    public static final String SUFIJO_MINIATURA = "_m";

    private final StorageProperties properties;
    private final ImagenSeguraProcesador procesador;

    /**
     * Valida, normaliza y guarda una imagen dentro de la subcarpeta indicada.
     *
     * @param archivo  imagen enviada por el cliente
     * @param basePath subcarpeta relativa a la raíz, armada por el servidor (ej. "negocios/14/productos/230")
     * @return metadata del archivo guardado (siempre image/jpeg)
     */
    public StoredFileDTO guardar(MultipartFile archivo, String basePath) {
        ImagenSeguraProcesador.ImagenProcesada imagen = procesador.procesar(archivo, true);

        Path raiz = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path directorio = raiz.resolve(basePath).normalize();
        if (!directorio.startsWith(raiz)) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", "Ruta de archivo no permitida."));
        }

        String uuid = GeneralUtil.generarUUIDv4().toString();
        String nombre = uuid + ".jpg";
        Path destino = directorio.resolve(nombre);
        Path destinoMiniatura = directorio.resolve(uuid + SUFIJO_MINIATURA + ".jpg");

        try {
            Files.createDirectories(directorio);
            escribirAtomico(destino, imagen.principal());
            escribirAtomico(destinoMiniatura, imagen.miniatura());
        } catch (IOException ex) {
            // Si algo falla a medias no se dejan archivos huérfanos
            borrarSilencioso(destino);
            borrarSilencioso(destinoMiniatura);
            throw new ApiResponseException(ApiCodeResponse.INTERNAL_ERROR, "No fue posible guardar la imagen.");
        }

        return StoredFileDTO.builder()
                .fileName(nombre)
                .originalFileName(limpiarNombreOriginal(archivo.getOriginalFilename()))
                .contentType("image/jpeg")
                .size((long) imagen.principal().length)
                .path(destino.toString())
                .relativePath(basePath)
                .build();
    }

    /**
     * Elimina del disco la imagen y su miniatura. No falla si ya no existen.
     *
     * @param rutaRelativa ruta relativa completa de la imagen principal (ej. "negocios/14/productos/230/abc.jpg")
     */
    public void eliminar(String rutaRelativa) {
        Path raiz = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path principal = raiz.resolve(rutaRelativa).normalize();
        if (!principal.startsWith(raiz)) return;
        borrarSilencioso(principal);
        String nombre = principal.getFileName().toString();
        if (nombre.endsWith(".jpg")) {
            borrarSilencioso(principal.resolveSibling(nombre.substring(0, nombre.length() - 4) + SUFIJO_MINIATURA + ".jpg"));
        }
    }

    // Escribe a un temporal y lo mueve al destino, para no dejar un archivo incompleto si se interrumpe
    private void escribirAtomico(Path destino, byte[] contenido) throws IOException {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.write(temporal, contenido);
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    private void borrarSilencioso(Path p) {
        try {
            Files.deleteIfExists(p);
            Files.deleteIfExists(p.resolveSibling(p.getFileName() + ".tmp"));
        } catch (IOException ignorado) {
            // Sin acción: es limpieza de mejor esfuerzo
        }
    }

    // El nombre original solo es informativo (no se usa en rutas): se quita cualquier ruta y caracteres de control
    private String limpiarNombreOriginal(String nombre) {
        if (nombre == null) return null;
        String soloNombre = nombre.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        return soloNombre.length() > 200 ? soloNombre.substring(0, 200) : soloNombre;
    }
}
