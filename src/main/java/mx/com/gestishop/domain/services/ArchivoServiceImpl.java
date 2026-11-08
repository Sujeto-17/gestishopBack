package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.ArchivoResponseDTO;
import mx.com.gestishop.core.dto.StoredFileDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.core.interfaces.FileStorageService;
import mx.com.gestishop.core.service.AlmacenImagenes;
import mx.com.gestishop.domain.interfaces.ArchivoService;
import mx.com.gestishop.domain.model.Archivo;
import mx.com.gestishop.domain.model.CatTipoEntidadArchivo;
import mx.com.gestishop.domain.repository.ArchivoRepository;
import mx.com.gestishop.domain.repository.CatTipoEntidadArchivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcula la subcarpeta por entidad, delega la validación y el guardado físico a AlmacenImagenes
 * (solo JPG/PNG reales, siempre re-codificados a JPEG) y persiste la metadata en la tabla archivos
 */
@Service
@RequiredArgsConstructor
public class ArchivoServiceImpl implements ArchivoService {

    private final ArchivoRepository archivoRepository;
    private final CatTipoEntidadArchivoRepository catTipoEntidadArchivoRepository;
    private final AlmacenImagenes almacenImagenes;

    @Override
    @Transactional
    public ArchivoResponseDTO subir(String claveTipoEntidad, Long idEntidad, String categoria,
                                    MultipartFile archivo, boolean marcarComoPrincipal) {
        // Subcarpeta organizada por módulo + id de entidad: ej. "negocios/14"
        return guardar(claveTipoEntidad, idEntidad, categoria, archivo, marcarComoPrincipal,
                claveTipoEntidad + "s/" + idEntidad);
    }

    @Override
    @Transactional
    public ArchivoResponseDTO subirDeNegocio(String claveTipoEntidad, Long idNegocio, Long idEntidad, String categoria,
                                             MultipartFile archivo, boolean marcarComoPrincipal) {
        // Ej. "negocios/14/productos/230": todo lo de un negocio queda bajo su propia carpeta
        return guardar(claveTipoEntidad, idEntidad, categoria, archivo, marcarComoPrincipal,
                "negocios/" + idNegocio + "/" + claveTipoEntidad + "s/" + idEntidad);
    }

    @Override
    public List<ArchivoResponseDTO> listar(String claveTipoEntidad, Long idEntidad, String categoria) {
        return RepositoryExecutor.execute(
                        () -> archivoRepository.listarPorEntidadYCategoria(claveTipoEntidad, idEntidad, categoria),
                        "Archivo", "listar")
                .stream().map(this::toResponse).toList();
    }

    // Flujo común: busca el tipo, guarda la imagen validada, desmarca la principal anterior y persiste la metadata
    private ArchivoResponseDTO guardar(String claveTipoEntidad, Long idEntidad, String categoria,
                                       MultipartFile archivo, boolean marcarComoPrincipal, String basePath) {
        CatTipoEntidadArchivo tipo = RepositoryExecutor.execute(
                () -> catTipoEntidadArchivoRepository.buscarPorClave(claveTipoEntidad)
                        .orElseThrow(() -> ErrorFactory.notFound("CatTipoEntidadArchivo", claveTipoEntidad)),
                "CatTipoEntidadArchivo", "consultar"
        );

        // Validación estricta + re-codificación + escritura en disco (lanza excepción si no es imagen válida)
        StoredFileDTO guardado = almacenImagenes.guardar(archivo, basePath);

        try {
            if (marcarComoPrincipal) {
                // La principal anterior deja de ser "la vigente", pero no se borra físicamente
                archivoRepository.listarPorEntidadYCategoria(claveTipoEntidad, idEntidad, categoria)
                        .forEach(a -> a.setEsPrincipal(false));
            }

            Archivo entidad = Archivo.builder()
                    .tipoEntidad(tipo)
                    .idEntidad(idEntidad)
                    .categoria(categoria)
                    .nombreArchivo(guardado.getFileName())
                    .nombreOriginal(guardado.getOriginalFileName())
                    .contentType(guardado.getContentType())
                    .tamanioBytes(guardado.getSize())
                    .rutaRelativa(guardado.getRelativePath())
                    .esPrincipal(marcarComoPrincipal)
                    .build();

            Archivo persistido = RepositoryExecutor.execute(
                    () -> archivoRepository.save(entidad), "Archivo", "subir");
            return toResponse(persistido);
        } catch (RuntimeException ex) {
            // Si la BD falla, el archivo recién escrito no debe quedar huérfano en disco
            almacenImagenes.eliminar(guardado.getRelativePath() + "/" + guardado.getFileName());
            throw ex;
        }
    }

    private ArchivoResponseDTO toResponse(Archivo a) {
        String ruta = a.getRutaRelativa() + "/" + a.getNombreArchivo();
        return ArchivoResponseDTO.builder()
                .uuidArchivo(a.getUuidArchivo())
                .nombreOriginal(a.getNombreOriginal())
                .contentType(a.getContentType())
                .rutaRelativa(ruta)
                .rutaMiniatura(ruta.endsWith(".jpg")
                        ? ruta.substring(0, ruta.length() - 4) + AlmacenImagenes.SUFIJO_MINIATURA + ".jpg"
                        : null)
                .esPrincipal(a.getEsPrincipal())
                .build();
    }
}
