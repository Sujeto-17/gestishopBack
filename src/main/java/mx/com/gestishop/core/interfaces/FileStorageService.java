package mx.com.gestishop.core.interfaces;

import java.util.List;
import java.util.Map;

public interface FileStorageService {

    // Generar reportes en pdf para multiples hojas
    byte[] generarReportePdfMultiple(String basePath, List<String> reportNames, Map<String, Object> parametros, List<?> data);
}
