package mx.com.gestishop.core.interfaces;

import mx.com.gestishop.core.dto.StoredFileDTO;
import mx.com.gestishop.core.dto.StoredImageDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileStorageService {

    // Generar reportes en pdf para multiples hojas
    byte[] generarReportePdfMultiple(String basePath, List<String> reportNames, Map<String, Object> parametros, List<?> data);

    StoredFileDTO save(MultipartFile multipartFile, String basePath);

    StoredFileDTO saveImagenComprimida(MultipartFile multipartFile, String basePath);

    Resource getUrlResource(String basePathFile);

    String getBase64(String basePathFile);

    String getBase64DataUri(String basePathFile);

    StoredImageDTO getImage(String basePathFile);
}
