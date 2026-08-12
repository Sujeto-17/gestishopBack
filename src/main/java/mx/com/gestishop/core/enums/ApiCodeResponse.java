package mx.com.gestishop.core.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import mx.com.gestishop.core.generic.ApiCode;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

/** Enum para representar los códigos de respuesta de la API
 * Cada código tiene un código, un estado HTTP y una descripción
 */
@Getter
@Schema(name = "ApiCodeResponse", description = "Códigos de respuesta de la API")
public enum ApiCodeResponse implements ApiCode {

    // ==============================
    // SATISFACTORIO
    // ==============================
    SUCCESS("OK", HttpStatus.OK, "Petición satisfactoria", ApiCodeType.SUCCESS),
    RESOURCE_FOUND("OK_RF", HttpStatus.OK, "Recurso encontrado", ApiCodeType.SUCCESS),
    RESOURCE_CREATED("OK_RC", HttpStatus.CREATED, "Recurso creado satisfactoriamente", ApiCodeType.SUCCESS),
    RESOURCE_UPDATED("OK_RU", HttpStatus.OK, "Recurso actualizado satisfactoriamente", ApiCodeType.SUCCESS),
    RESOURCE_DELETED("OK_RD", HttpStatus.OK, "Recurso eliminado satisfactoriamente", ApiCodeType.SUCCESS),
    NOT_FOUND("OK_NF", HttpStatus.OK, "No se encontraron registros.", ApiCodeType.SUCCESS),

    // ==============================
    // ERROR DE NEGOCIO
    // ==============================
    REQUIRED_DATA("BE_RD", HttpStatus.BAD_REQUEST, "Dato requerido", ApiCodeType.BUSINESS_ERROR),
    RESOURCE_NOT_FOUND("BE_RNF", HttpStatus.NOT_FOUND, "Recurso no encontrado", ApiCodeType.BUSINESS_ERROR),
    CONFLICT("BE_C", HttpStatus.CONFLICT, "Conflicto con la petición", ApiCodeType.BUSINESS_ERROR),

    // ==============================
    // ERROR TÉCNICO
    // ==============================
    INTERNAL_ERROR("TE_IE", HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", ApiCodeType.TECHNICAL_ERROR),
    DATABASE_ERROR("TE_DBE", HttpStatus.INTERNAL_SERVER_ERROR, "Error en base de datos", ApiCodeType.TECHNICAL_ERROR),
    DATABASE_UNAVAILABLE("TE_DBU", HttpStatus.SERVICE_UNAVAILABLE, "Base de datos no disponible", ApiCodeType.TECHNICAL_ERROR),
    BAD_REQUEST("TE_BR", HttpStatus.BAD_REQUEST, "Solicitud incorrecta ", ApiCodeType.TECHNICAL_ERROR),
    RESOURCE_UNAVAILABLE("TE_RU", HttpStatus.SERVICE_UNAVAILABLE, "Recurso no disponible", ApiCodeType.TECHNICAL_ERROR),
    TIMEOUT("TE_TO", HttpStatus.GATEWAY_TIMEOUT, "Tiempo de espera agotado", ApiCodeType.TECHNICAL_ERROR);

    private final String codigo;
    private final HttpStatus httpStatus;
    private final String mensaje;
    private final ApiCodeType tipo;

    /** Constructor del enum
     * @param codigo el código de respuesta
     * @param httpStatus el estado HTTP asociado al código
     * @param mensaje la descripción del código
     * @param tipo el tipo de código (éxito, error de negocio, error técnico)
     */
    ApiCodeResponse(String codigo, HttpStatus httpStatus, String mensaje, ApiCodeType tipo) {
        this.codigo = codigo;
        this.httpStatus = httpStatus;
        this.mensaje = mensaje;
        this.tipo = tipo;
    }

    /** Metodo para obtener el enum a partir del código
     * @param codigo el código de respuesta
     * @return el enum correspondiente al código
     * @throws IllegalArgumentException si el código no es válido
     */
    public static ApiCodeResponse fromCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(c -> c.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Código no válido"));
    }

}
