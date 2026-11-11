package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.response.FotoProductoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fotos de los productos: una principal y hasta 4 más (5 en total). Las operaciones de escritura devuelven
 * la lista completa y ya ordenada, para que el cliente solo reemplace lo que muestra.
 */
public interface ProductoFotoService {

    // Máximo de fotos por producto (1 principal + 4 más)
    int MAX_FOTOS = 5;

    // Fotos del producto: primero la principal y luego las demás por su orden
    List<FotoProductoResponseDTO> listar(UUID uuidProducto, Long idNegocio);

    // Sube una foto; la primera que se sube queda como principal
    List<FotoProductoResponseDTO> subir(UUID uuidProducto, MultipartFile archivo, Long idNegocio);

    // Pasa la foto a ser la principal; la principal anterior queda entre las demás
    List<FotoProductoResponseDTO> marcarPrincipal(UUID uuidProducto, UUID uuidFoto, Long idNegocio);

    // Define el orden de las fotos; la lista debe traer todas las del producto
    List<FotoProductoResponseDTO> reordenar(UUID uuidProducto, List<UUID> uuidsFotos, Long idNegocio);

    // Borra una foto (y su miniatura del disco); si era la principal, pasa a serlo la siguiente
    List<FotoProductoResponseDTO> eliminar(UUID uuidProducto, UUID uuidFoto, Long idNegocio);

    // Borra todas las fotos de un producto (al eliminar el producto)
    void eliminarTodas(Long idProducto);

    // Miniatura de la foto principal de cada producto del negocio (idProducto → ruta relativa), para el listado
    Map<Long, String> miniaturasPrincipales(Long idNegocio);
}