package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.ArchivoResponseDTO;
import mx.com.gestishop.core.dto.StoredFileDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.core.interfaces.FileStorageService;
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
 * Implementación genérica: calcula la subcarpeta por módulo+entidad
 * (ej. "negocios/14"), delega el guardado físico a FileStorageService
 * (con compresión si es imagen), y persiste la metadata en la tabla archivos.
 */
@Service
@RequiredArgsConstructor
public class ArchivoServiceImpl implements ArchivoService {

    private final ArchivoRepository archivoRepository;
    private final CatTipoEntidadArchivoRepository catTipoEntidadArchivoRepository;

    private final FileStorageService fileStorageService;

    private static final Set<String> EXTENSIONES_BLOQUEADAS =
            Set.of(".exe", ".sh", ".bat", ".cmd", ".msi", ".jar", ".php", ".js");

    @Override
    @Transactional
    public ArchivoResponseDTO subir(String claveTipoEntidad, Long idEntidad, String categoria,
                                    MultipartFile archivo, boolean marcarComoPrincipal) {

        validarExtensionSegura(archivo.getOriginalFilename());

        CatTipoEntidadArchivo tipo = RepositoryExecutor.execute(
                () -> catTipoEntidadArchivoRepository.buscarPorClave(claveTipoEntidad)
                        .orElseThrow(() -> ErrorFactory.notFound("CatTipoEntidadArchivo", claveTipoEntidad)),
                "CatTipoEntidadArchivo", "consultar"
        );

        // Subcarpeta organizada por módulo + id de entidad: ej. "negocios/14"
        String basePath = claveTipoEntidad + "s/" + idEntidad;

        // Si es imagen y pesa, se comprime automáticamente antes de guardar
        StoredFileDTO guardado = fileStorageService.saveImagenComprimida(archivo, basePath);

        if (marcarComoPrincipal) {
            // Desmarca cualquier principal previo de esta misma entidad+categoría
            // (ej. el logo anterior deja de ser "el vigente", pero no se borra físicamente)
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
    }

    @Override
    public List<ArchivoResponseDTO> listar(String claveTipoEntidad, Long idEntidad, String categoria) {
        return RepositoryExecutor.execute(
                        () -> archivoRepository.listarPorEntidadYCategoria(claveTipoEntidad, idEntidad, categoria),
                        "Archivo", "listar")
                .stream().map(this::toResponse).toList();
    }

    private ArchivoResponseDTO toResponse(Archivo a) {
        return ArchivoResponseDTO.builder()
                .uuidArchivo(a.getUuidArchivo())
                .nombreOriginal(a.getNombreOriginal())
                .contentType(a.getContentType())
                .rutaRelativa(a.getRutaRelativa() + "/" + a.getNombreArchivo())
                .esPrincipal(a.getEsPrincipal())
                .build();
    }

    private void validarExtensionSegura(String nombreOriginal) {
        if (nombreOriginal == null) return;
        String nombre = nombreOriginal.toLowerCase();
        boolean bloqueada = EXTENSIONES_BLOQUEADAS.stream().anyMatch(nombre::endsWith);
        if (bloqueada) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Tipo de archivo no permitido."));
        }
    }
}
