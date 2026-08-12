package mx.com.gestishop.core.enums;

import io.swagger.v3.oas.annotations.media.Schema;

// Enum que representa los tipos de códigos de respuesta de la API
@Schema(name = "ApiCodeType", description = "Tipo de código de respuesta")
public enum ApiCodeType {

    @Schema(description = "Operación exitosa")
    SUCCESS,
    @Schema(description = "Error de negocio")
    BUSINESS_ERROR,
    @Schema(description = "Error técnico del sistema")
    TECHNICAL_ERROR
}
