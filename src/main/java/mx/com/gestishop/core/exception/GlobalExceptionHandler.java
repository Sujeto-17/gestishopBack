package mx.com.gestishop.core.exception;

import lombok.extern.slf4j.Slf4j;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.ApiResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Clase que maneja las excepciones de tipo ApiResponseException a nivel global
 * en la aplicación. Utiliza la anotación @RestControllerAdvice para interceptar
 * las excepciones lanzadas por los controladores REST y construir respuestas
 * HTTP adecuadas utilizando ApiResponseBuilder.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja las excepciones de tipo ApiResponseException y construye una
     * respuesta HTTP utilizando ApiResponseBuilder.
     *
     * @param ex La excepción de tipo ApiResponseException que se ha lanzado.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * error y los datos proporcionados en la excepción.
     */
    @ExceptionHandler(ApiResponseException.class)
    public ResponseEntity<ApiDataResponseDTO<Object>> handleApiException(ApiResponseException ex) {
        log.warn("API ERROR: {} - data: {}",
                ex.getApiCodeResponse().getCodigo(),
                ex.getData());

        return ApiResponseBuilder.build(ex.getApiCodeResponse(), ex.getData());
    }

}
