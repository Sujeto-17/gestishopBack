package mx.com.gestishop.core.generic;

import mx.com.gestishop.core.enums.ApiCodeType;
import org.springframework.http.HttpStatus;

// Interfaz que define la estructura de los códigos de respuesta de la API
public interface ApiCode {

    String getCodigo();
    HttpStatus getHttpStatus();
    String getMensaje();
    ApiCodeType getTipo();
}
