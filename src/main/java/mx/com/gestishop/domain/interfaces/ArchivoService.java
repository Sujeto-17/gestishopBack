package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.response.ArchivoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Gestión genérica de archivos adjuntos para cualquier módulo. Combina
 * FileStorageService (disco) con la tabla archivos (metadata en BD).
 */
public interface ArchivoService {

    /**
     * Sube un archivo para una entidad específica. Si marcarComoPrincipal es true
     * (ej. el logo de un negocio), desmarca cualquier otro archivo principal previo
     * de esa misma entidad+categoría antes de guardar el nuevo.
     */
    ArchivoResponseDTO subir(String claveTipoEntidad, Long idEntidad, String categoria,
                             MultipartFile archivo, boolean marcarComoPrincipal);

    List<ArchivoResponseDTO> listar(String claveTipoEntidad, Long idEntidad, String categoria);
}
