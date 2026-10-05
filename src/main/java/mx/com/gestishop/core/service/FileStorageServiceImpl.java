package mx.com.gestishop.core.service;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.dto.StoredFileDTO;
import mx.com.gestishop.core.dto.StoredImageDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.interfaces.FileStorageService;
import mx.com.gestishop.core.utils.GeneralUtil;
import mx.com.gestishop.infrastructure.config.StorageProperties;
import net.coobird.thumbnailator.Thumbnails;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final StorageProperties properties;

    private static final long UMBRAL_COMPRESION_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final int DIMENSION_MAXIMA_PX = 1920;
    private static final float CALIDAD_JPEG = 0.8f;

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

    /**
     * Guarda un archivo recibido (multipart) dentro de una subcarpeta indicada por basePath.
     *
     * @param multipartFile archivo enviado por el cliente (ej. una foto de evidencia)
     * @param basePath      subcarpeta dentro del directorio raíz donde se almacenará
     *                      (ej. "evidencias/siembra")
     * @return DTO con la metadata del archivo ya guardado (nombre generado, nombre original,
     * tipo de contenido, tamaño, ruta absoluta y ruta relativa)
     */
    public StoredFileDTO save(MultipartFile multipartFile, String basePath) {
        // Validación de datos obligatorios: se arma una lista de qué campos faltan
        // para poder reportarlos todos juntos en un solo error, en vez de fallar
        // uno por uno
        List<String> lstRequeridos = new ArrayList<>();


        // Si falta algún dato requerido, se corta la ejecución con un error que indica exactamente qué campos faltaron
        if (!lstRequeridos.isEmpty()) {
            throw new ApiResponseException(
                    ApiCodeResponse.REQUIRED_DATA,
                    lstRequeridos
            );
        }

        try {
            String rootPath = properties.getUploadDir(); // Directorio raíz configurado (ej. /var/app/uploads)
            String originalFilename = multipartFile.getOriginalFilename(); // Nombre original del archivo
            String cleanName = StringUtils.cleanPath(originalFilename); // Limpia el nombre para evitar path traversal
            String extension = getExtension(originalFilename);  // Extrae la extensión del archivo original (ej. ".jpg", ".png")
            String generatedName = GeneralUtil.generarUUIDv4() + extension; // Genera un nombre de archivo único (UUID) + su extensión

            // Los archivos se guardan directo en rootPath/basePath
            Path directory = Paths.get(
                    rootPath,
                    basePath
            );

            Files.createDirectories(directory); // Crea la carpeta destino si no existe (incluyendo carpetas intermedias)
            Path fullPath = directory.resolve(generatedName); // Ruta final completa del archivo: directorio + nombre generado

            // Copia el contenido del archivo subido al disco en la ruta calculada
            Files.copy(
                    multipartFile.getInputStream(),
                    fullPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return StoredFileDTO.builder()
                    .fileName(generatedName)
                    .originalFileName(cleanName)
                    .contentType(multipartFile.getContentType())
                    .size(multipartFile.getSize())
                    .path(fullPath.toString())
                    .relativePath(
                            basePath
                    )
                    .build();
        } catch (Exception ex) {
            throw new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    ex.getMessage()
            );
        }
    }

    /**
     * Si el archivo es una imagen y pesa más que UMBRAL_COMPRESION_BYTES, la redimensiona y
     * recomprime antes de guardar, para reducir espacio en disco sin pérdida visual perceptible.
     */
    public StoredFileDTO saveImagenComprimida(MultipartFile multipartFile, String basePath) {
        if (debeComprimirse(multipartFile)) {
            multipartFile = comprimirImagen(multipartFile);
        }
        return save(multipartFile, basePath);
    }

    private boolean debeComprimirse(MultipartFile multipartFile) {
        String contentType = multipartFile.getContentType();
        return contentType != null
                && contentType.startsWith("image/")
                && multipartFile.getSize() > UMBRAL_COMPRESION_BYTES;
    }

    private MultipartFile comprimirImagen(MultipartFile foto) {
        try {
            java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();

            Thumbnails.of(foto.getInputStream())
                    .size(DIMENSION_MAXIMA_PX, DIMENSION_MAXIMA_PX)
                    .outputQuality(CALIDAD_JPEG)
                    .outputFormat("jpg")
                    .toOutputStream(salida);

            byte[] bytesComprimidos = salida.toByteArray();
            String nombreOriginal = StringUtils.cleanPath(
                    Objects.requireNonNullElse(foto.getOriginalFilename(), "imagen")
            );

            return new BytesMultipartFile(bytesComprimidos, nombreOriginal, "image/jpeg");

        } catch (IOException ex) {
            throw new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    "No fue posible procesar la imagen: " + ex.getMessage()
            );
        }
    }

    /**
     * Resuelve y valida la ruta de un archivo ya guardado, devolviéndolo como Resource
     * (útil para servirlo directo en una respuesta HTTP, ej. como descarga o streaming).
     *
     * @param basePathFile ruta relativa del archivo (subcarpeta + nombre de archivo)
     */
    public Resource getUrlResource(String basePathFile) {
        String rootPath = properties.getUploadDir();

        try {
            // Ruta raíz normalizada y absoluta, usada como "límite" permitido
            Path root = Paths.get(rootPath)
                    .toAbsolutePath()
                    .normalize();

            // Ruta final del archivo solicitado, también normalizada
            Path path = root
                    .resolve(basePathFile)
                    .normalize();

            // Verificación de seguridad (path traversal): si la ruta resuelta se "escapa" del directorio raíz permitido, se rechaza
            if (!path.startsWith(root)) {
                throw new ApiResponseException(
                        ApiCodeResponse.CONFLICT,
                        "Ruta de archivo no permitida"
                );
            }

            Resource resource = new UrlResource(path.toUri());

            // Verifica que el archivo realmente exista y se pueda leer antes de regresarlo
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiResponseException(
                        ApiCodeResponse.CONFLICT,
                        "Archivo no encontrado"
                );
            }

            return resource;
        } catch (MalformedURLException ex) {
            throw new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    "URL con formato incorrecto"
            );
        }
    }

    /**
     * Devuelve el contenido del archivo codificado en Base64 puro (sin prefijo de Data URI).
     * Útil cuando el consumidor ya sabe el tipo de contenido y solo necesita los bytes.
     */
    public String getBase64(String basePathFile) {
        try {
            Resource resource = getUrlResource(basePathFile);
            byte[] bytes = resource.getContentAsByteArray();

            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException ex) {
            throw new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    "No fue posible recuperar el archivo"
            );
        }
    }

    /**
     * Devuelve el archivo como una Data URI completa (ej. "data:image/jpeg;base64,...."),
     * lista para usarse directo en un <img src="..."> del frontend sin llamadas adicionales.
     */
    public String getBase64DataUri(String basePathFile) {
        try {
            Resource resource = getUrlResource(basePathFile);
            byte[] bytes = resource.getContentAsByteArray();
            String base64 = Base64.getEncoder().encodeToString(bytes);

            // Intenta detectar el tipo MIME real del archivo (ej. image/png, image/jpeg)
            String contentType = Files.probeContentType(
                    Paths.get(resource.getURI())
            );

            // Si no se pudo determinar el tipo, se usa un genérico como respaldo
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return "data:" + contentType + ";base64," + base64;
        } catch (IOException ex) {
            throw new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    "No fue posible recuperar el archivo"
            );
        }
    }

    /**
     * Devuelve la imagen como bytes crudos junto con su contentType, sin codificar a Base64.
     * Útil para servir la imagen directamente en una respuesta HTTP (ej. ResponseEntity<byte[]>
     * con el header Content-Type correspondiente).
     */
    public StoredImageDTO getImage(String basePathFile) {
        try {
            Resource resource = getUrlResource(basePathFile);
            byte[] bytes = resource.getContentAsByteArray();
            String contentType = Files.probeContentType(
                    Paths.get(resource.getURI())
            );

            return StoredImageDTO.builder()
                    .bytes(bytes)
                    .contentType(contentType)
                    .build();
        } catch (IOException ex) {
            throw new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    "No fue posible recuperar la imagen"
            );
        }
    }

    /**
     * Extrae la extensión de un nombre de archivo (incluyendo el punto), ej. ".jpg".
     * Si el nombre es nulo o no tiene punto, regresa cadena vacía.
     */
    private String getExtension(String filename) {
        if (Objects.isNull(filename) || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf("."));
    }
}
