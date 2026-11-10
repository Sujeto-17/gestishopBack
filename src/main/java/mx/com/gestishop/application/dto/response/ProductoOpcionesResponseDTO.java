package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

// Lo que el formulario de productos necesita saber del negocio (mayoreo e IVA)
@Getter
@Builder
@Schema(name = "ProductoOpcionesResponseDTO")
public class ProductoOpcionesResponseDTO {

    @Schema(description = "true si el negocio (tipo tienda) maneja precio de mayoreo en productos")
    private boolean permiteMayoreo;

    @Schema(description = "true si el negocio emite factura; solo entonces el formulario muestra la tasa de IVA")
    private boolean emiteFactura;
}


