package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Datos para crear o editar un proveedor. Los contactos y descuentos viajan completos: lo que no venga se elimina.
@Getter
@Setter
@Schema(name = "ProveedorRequestDTO", description = "Proveedor del negocio con sus contactos y descuentos")
public class ProveedorRequestDTO {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 100)
    @Schema(example = "Papelera del Sureste")
    private String nombre;

    @Size(max = 150)
    @Schema(nullable = true)
    private String razonSocial;

    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC no es válido")
    @Schema(example = "XAXX010101000", nullable = true)
    private String rfc;

    @Size(max = 200)
    @Schema(description = "Qué vende, de un vistazo", example = "papelería, vinil, sublimación", nullable = true)
    private String resumenSurtido;

    // --- Facturación ---
    @Schema(description = "true si el proveedor puede facturar", defaultValue = "false")
    private Boolean factura = false;

    @Schema(description = "true = sus precios ya incluyen IVA; false = el IVA se suma al facturar", defaultValue = "true")
    private Boolean preciosIncluyenIva = true;

    // --- Envíos ---
    @Schema(description = "true si el proveedor hace envíos", defaultValue = "false")
    private Boolean haceEnvios = false;

    @DecimalMin(value = "0.00", message = "El costo de envío no puede ser negativo")
    @Digits(integer = 8, fraction = 2)
    @Schema(description = "Solo aplica si hace envíos", nullable = true)
    private BigDecimal costoEnvio;

    @DecimalMin(value = "0.00", message = "El monto de envío gratis no puede ser negativo")
    @Digits(integer = 8, fraction = 2)
    @Schema(description = "Compra mínima para envío gratis; solo aplica si hace envíos", nullable = true)
    private BigDecimal envioGratisDesde;

    // --- Condiciones de compra ---
    @DecimalMin(value = "0.00", message = "El pedido mínimo no puede ser negativo")
    @Digits(integer = 8, fraction = 2)
    @Schema(nullable = true)
    private BigDecimal pedidoMinimo;

    @Size(max = 100)
    @Schema(example = "Contado; MSI con tarjeta", nullable = true)
    private String formasPago;

    @Min(1)
    @Max(5)
    @Schema(description = "Apreciación interna de 1 a 5", nullable = true)
    private Short calificacion;

    // --- Ubicación y notas ---
    @Size(max = 200)
    @Schema(nullable = true)
    private String direccion;

    @Schema(description = "ID del municipio donde se ubica", nullable = true)
    private Long idMunicipio;

    @Size(max = 150)
    @Pattern(regexp = "^https?://\\S+$", message = "El sitio web debe iniciar con http:// o https://")
    @Schema(nullable = true)
    private String sitioWeb;

    @Size(max = 500)
    @Schema(nullable = true)
    private String notas;

    @Valid
    @Size(max = 10, message = "Máximo 10 contactos por proveedor")
    private List<ProveedorContactoRequestDTO> contactos = new ArrayList<>();

    @Valid
    @Size(max = 10, message = "Máximo 10 descuentos por proveedor")
    private List<ProveedorDescuentoRequestDTO> descuentos = new ArrayList<>();

    // Los opcionales llegan como "" cuando el formulario los deja vacíos: se guardan como null
    // (y el RFC en mayúsculas) para que las validaciones de formato no rechacen un campo en blanco.
    public void setNombre(String valor) { this.nombre = vacioANull(valor); }
    public void setRazonSocial(String valor) { this.razonSocial = vacioANull(valor); }
    public void setRfc(String valor) { String v = vacioANull(valor); this.rfc = v == null ? null : v.toUpperCase(); }
    public void setResumenSurtido(String valor) { this.resumenSurtido = vacioANull(valor); }
    public void setFormasPago(String valor) { this.formasPago = vacioANull(valor); }
    public void setDireccion(String valor) { this.direccion = vacioANull(valor); }
    public void setSitioWeb(String valor) { this.sitioWeb = vacioANull(valor); }
    public void setNotas(String valor) { this.notas = vacioANull(valor); }
    // Si el formulario no manda estos datos se conservan los valores por defecto
    public void setFactura(Boolean valor) { this.factura = valor != null && valor; }
    public void setPreciosIncluyenIva(Boolean valor) { this.preciosIncluyenIva = valor == null || valor; }
    public void setHaceEnvios(Boolean valor) { this.haceEnvios = valor != null && valor; }
    public void setContactos(List<ProveedorContactoRequestDTO> valor) { this.contactos = valor != null ? valor : new ArrayList<>(); }
    public void setDescuentos(List<ProveedorDescuentoRequestDTO> valor) { this.descuentos = valor != null ? valor : new ArrayList<>(); }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
