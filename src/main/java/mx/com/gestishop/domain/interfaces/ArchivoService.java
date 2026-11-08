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
     * Sube una imagen asociada a una entidad que cuelga directo de "negocios/{id}" (ej. el logo del negocio).
     * Si marcarComoPrincipal es true, desmarca cualquier otra principal previa de esa entidad+categoría.
     */
    ArchivoResponseDTO subir(String claveTipoEntidad, Long idEntidad, String categoria,
                             MultipartFile archivo, boolean marcarComoPrincipal);

    /**
     * Sube una imagen de una entidad que pertenece a un negocio (ej. producto, variante).
     * Se guarda en "negocios/{idNegocio}/{claveTipo}s/{idEntidad}". El idNegocio debe venir del token,
     * y quien llama debe haber comprobado ya que la entidad pertenece a ese negocio.
     */
    ArchivoResponseDTO subirDeNegocio(String claveTipoEntidad, Long idNegocio, Long idEntidad, String categoria,
                                      MultipartFile archivo, boolean marcarComoPrincipal);

    List<ArchivoResponseDTO> listar(String claveTipoEntidad, Long idEntidad, String categoria);
}
