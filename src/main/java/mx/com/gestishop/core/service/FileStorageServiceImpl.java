package mx.com.gestishop.core.service;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.interfaces.FileStorageService;
import mx.com.gestishop.infrastructure.config.StorageProperties;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final StorageProperties properties;

    /**
     * Genera un único PDF a partir de uno o varios archivos Jasper/JRXML.
     * @param basePath Carpeta dentro de resources/static donde están los .jrxml (ej. "programa/2026")
     * @param reportNames Lista de nombres de archivos, sin extensión (ej. List.of("vacaciones"))
     * @param parametros Mapa de parámetros globales para el reporte.
     * @param data Lista de datos para el reporte (dataSource).
     * @return byte[] del PDF final.
     */
    public byte[] generarReportePdfMultiple(String basePath, List<String> reportNames,
                                            Map<String, Object> parametros, List<?> data) {

        if (reportNames == null || reportNames.isEmpty()) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT, "No se proporcionaron nombres de reporte para generar.");
        }

        try {
            List<JasperPrint> jasperPrintList = new ArrayList<>();
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

            for (String name : reportNames) {
                String path = "static/" + basePath + "/" + name + ".jrxml";
                Resource resource = new ClassPathResource(path);

                if (!resource.exists()) {
                    throw new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND,
                            "El archivo " + name + " no se encontró en la ruta: " + path);
                }

                try (InputStream in = resource.getInputStream()) {
                    JasperReport compiledReport = JasperCompileManager.compileReport(in);
                    JasperPrint filledReport = JasperFillManager.fillReport(compiledReport, parametros, dataSource);
                    jasperPrintList.add(filledReport);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrintList));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));

            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            configuration.setCreatingBatchModeBookmarks(true);
            exporter.setConfiguration(configuration);

            exporter.exportReport();
            return baos.toByteArray();

        } catch (JRException | IOException e) {
            throw new ApiResponseException(ApiCodeResponse.INTERNAL_ERROR, "Error al procesar la exportación: " + e.getMessage());
        }
    }

    // Leer las imagenes guardadas
    public byte[] leerImagenClasspath(String path) {
        Resource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            throw new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND, "No se encontró la imagen: " + path);
        }

        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new ApiResponseException(ApiCodeResponse.INTERNAL_ERROR, "Error al leer la imagen: " + path);
        }
    }
}
