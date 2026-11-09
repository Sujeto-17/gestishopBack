package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Configuración de facturación e IVA del negocio
@Getter
@Setter
@Schema(name = "ConfiguracionFiscalRequestDTO")
public class ConfiguracionFiscalRequestDTO {

    @NotNull
    @Schema(description = "true si el negocio emite factura; false = el IVA no se calcula")
    private Boolean emiteFactura;

    @NotNull
    @Schema(description = "Solo aplica si emite factura: true = los precios ya incluyen IVA; false = el IVA se suma al facturar")
    private Boolean preciosIncluyenIva;
}