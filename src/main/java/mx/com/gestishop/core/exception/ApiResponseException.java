package mx.com.gestishop.core.exception;

import lombok.Getter;
import mx.com.gestishop.core.enums.ApiCodeResponse;

/**
 * Excepción personalizada para manejar errores en las respuestas de la API.
 * Esta excepción encapsula un código de respuesta específico y datos adicionales
 * relacionados con el error.
 */
@Getter
public class ApiResponseException extends RuntimeException {

    private final ApiCodeResponse apiCodeResponse;
    private final Object data;

    /** Constructor de la excepción sin datos adicionales.
     *
     * @param apiCodeResponse El código de respuesta que contiene el código,
     * mensaje y estado HTTP.
     */
    public ApiResponseException(ApiCodeResponse apiCodeResponse) {
        super(apiCodeResponse.getMensaje());
        this.apiCodeResponse = apiCodeResponse;
        this.data = null;
    }

    /** Constructor de la excepción con datos adicionales.
     *
     * @param apiCodeResponse El código de respuesta que contiene el código,
     * mensaje y estado HTTP.
     * @param data Datos adicionales relacionados con la excepción.
     */
    public ApiResponseException(ApiCodeResponse apiCodeResponse, Object data) {
        super(apiCodeResponse.getMensaje());
        this.apiCodeResponse = apiCodeResponse;
        this.data = data;
    }

    public ApiResponseException(ApiCodeResponse apiCodeResponse, Object data, Throwable cause) {
        super(apiCodeResponse.getMensaje(), cause);
        this.apiCodeResponse = apiCodeResponse;
        this.data = data;
    }
}
