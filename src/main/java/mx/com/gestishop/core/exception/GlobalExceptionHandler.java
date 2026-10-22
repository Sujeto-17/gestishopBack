package mx.com.gestishop.core.exception;

import lombok.extern.slf4j.Slf4j;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.generic.ApiResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Manejo global de errores de los controllers REST.
 * Todo error sale con el mismo formato (ApiDataResponseDTO: codigo, mensaje, tipo, datos),
 * para que el front siempre pueda leer "datos.detalle" y mostrarlo al usuario.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Errores de negocio propios (conflicto, no encontrado, datos requeridos...)
     * Es el handler principal: casi todo el código lanza ApiResponseException
     */
    @ExceptionHandler(ApiResponseException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleApiException(ApiResponseException ex) {
        log.warn("API ERROR: {} - data: {}", ex.getApiCodeResponse().getCodigo(), ex.getData());
        return ApiResponseBuilder.build(ex.getApiCodeResponse(), ex.getData());
    }

    /**
     * Falla alguna validación del body (@Valid en el controller: @NotBlank, @Pattern, @Size...).
     * Antes salía la respuesta por defecto de Spring, sin 'codigo', y el front la mostraba
     * como "No se pudo conectar con el servidor".
     * 'detalle' trae los mensajes (para mostrar) y 'campos' el mensaje de cada campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));

        // Mensajes sin repetir, en orden de aparición
        String detalle = String.join(" ", new LinkedHashSet<>(campos.values()));

        log.warn("VALIDACION: {}", campos);
        Object datos = Map.of("detalle", detalle, "campos", campos);
        return ApiResponseBuilder.build(ApiCodeResponse.REQUIRED_DATA, datos);
    }

    /** JSON mal formado, fecha con formato inválido, texto donde se esperaba un número, etc. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleBodyIlegible(HttpMessageNotReadableException ex) {
        log.warn("BODY ILEGIBLE: {}", ex.getMessage());
        Object datos = Map.of("detalle", "Los datos enviados no tienen un formato válido.");
        return ApiResponseBuilder.build(ApiCodeResponse.BAD_REQUEST, datos);
    }

    /** Un parámetro de la URL no tiene el tipo esperado (por ejemplo un UUID mal escrito). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        log.warn("PARAMETRO INVALIDO: {}", ex.getName());
        Object datos = Map.of("detalle", "El parámetro '" + ex.getName() + "' no tiene un formato válido.");
        return ApiResponseBuilder.build(ApiCodeResponse.BAD_REQUEST, datos);
    }

    /** Se subió un archivo más grande que spring.servlet.multipart.max-file-size (5 MB). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleArchivoGrande(MaxUploadSizeExceededException ex) {
        Object datos = Map.of("detalle", "El archivo supera el tamaño máximo permitido (5 MB).");
        return ApiResponseBuilder.build(ApiCodeResponse.REQUIRED_DATA, datos);
    }

    /**
     * Un @PreAuthorize rechazó la petición (usuario sin el rol o el módulo requerido).
     * Se responde 403 con el formato estándar.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleAccesoDenegado(AccessDeniedException ex) {
        log.warn("ACCESO DENEGADO: {}", ex.getMessage());
        return ApiResponseBuilder.build(ApiCodeResponse.FORBIDDEN, null);
    }
}