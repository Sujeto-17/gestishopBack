package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import mx.com.gestishop.core.dto.CatalogoRefDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Schema(name = "ProveedorResponseDTO")
public class ProveedorResponseDTO {
    private UUID uuidProveedor;
    private String nombre;
    private String razonSocial;
    private String rfc;
    private String resumenSurtido;

    private boolean factura;
    private boolean preciosIncluyenIva;

    private boolean haceEnvios;
    private BigDecimal costoEnvio;
    private BigDecimal envioGratisDesde;

    private BigDecimal pedidoMinimo;
    private String formasPago;
    private Short calificacion;

    private String direccion;
    private CatalogoRefDTO municipio;
    private String sitioWeb;
    private String notas;
    private String estatus;

    @Schema(description = "Cantidad de variantes (productos) que surte este proveedor")
    private int totalVariantes;

    private List<ProveedorContactoResponseDTO> contactos;

    @Schema(description = "Solo viene en el detalle; en la lista se omite")
    private List<ProveedorDescuentoResponseDTO> descuentos;
}
