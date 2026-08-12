package mx.com.gestishop.core.generic;

import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.dto.ApiPageResponseDTO;
import mx.com.gestishop.core.dto.PageMetadataDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

// Clase utilitaria para construir respuestas HTTP estándar para la API
public class ApiResponseBuilder {

    private ApiResponseBuilder() {
    }

    /**
     * Construye una respuesta HTTP estándar para la API.
     *
     * @param apiCodeResponse El código de respuesta que contiene el código, mensaje
     *                        y estado HTTP.
     * @param data            Los datos adicionales que se incluirán en la respuesta (opcional).
     * @param <T>             El tipo de los datos adicionales.
     * @return Una ResponseEntity con el ApiResponseDTO construido y el estado HTTP
     * correspondiente.
     */
    public static <T> ResponseEntity<ApiDataResponseDTO<T>> build(ApiCodeResponse apiCodeResponse,
                                                                  T data) {
        ApiDataResponseDTO<T> response = ApiDataResponseDTO.<T>builder()
                .codigo(apiCodeResponse.getCodigo())
                .mensaje(apiCodeResponse.getMensaje())
                .tipo(apiCodeResponse.getTipo().name())
                .datos(data)
                .build();

        return ResponseEntity
                .status(apiCodeResponse.getHttpStatus())
                .body(response);
    }

    /**
     * Construye una respuesta HTTP estándar para la API con paginación.
     *
     * @param apiCodeResponse El código de respuesta que contiene el código, mensaje
     *                        y estado HTTP.
     * @param page            La página de datos que se incluirá en la respuesta.
     * @param <T>             El tipo de los datos en la página.
     * @return Una ResponseEntity con el ApiPageResponseDTO construido y el estado
     * HTTP correspondiente.
     */
    public static <T> ResponseEntity<ApiPageResponseDTO<T>> buildPage(ApiCodeResponse apiCodeResponse,
                                                                      Page<T> page) {
        ApiPageResponseDTO<T> response = ApiPageResponseDTO.<T>builder()
                .codigo(apiCodeResponse.getCodigo())
                .mensaje(apiCodeResponse.getMensaje())
                .tipo(apiCodeResponse.getTipo().name())
                .datos(page.getContent())
                .page(PageMetadataDTO.from(page))
                .build();

        return ResponseEntity
                .status(apiCodeResponse.getHttpStatus())
                .body(response);
    }
}
