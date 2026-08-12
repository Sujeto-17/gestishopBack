package mx.com.gestishop.core.generic;

import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;

import java.util.Map;

public class ErrorFactory {

    public static ApiResponseException notFound(String recurso) {
        return new ApiResponseException(
                ApiCodeResponse.RESOURCE_NOT_FOUND,
                Map.of(
                        "recurso", recurso
                )
        );
    }

    public static ApiResponseException notFound(String recurso, Object id) {
        return new ApiResponseException(
                ApiCodeResponse.RESOURCE_NOT_FOUND,
                Map.of(
                        "recurso", recurso,
                        "id", id
                )
        );
    }

    public static ApiResponseException unavailable(String recurso) {
        return new ApiResponseException(
                ApiCodeResponse.RESOURCE_UNAVAILABLE,
                Map.of(
                        "recurso", recurso
                )
        );
    }
}
