package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

// Datos para dar de alta un negocio nuevo en la plataforma
@Getter
@Setter
@Schema(name = "NegocioRequestDTO", description = "Datos para registrar un negocio")
public class NegocioRequestDTO {

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Nombre del negocio", example = "The Julian's")
    private String nombre;

    @Schema(description = "URL del logo, opcional", nullable = true)
    private String logoUrl;

    @NotBlank
    @Schema(description = "Tipo de flujo operativo", example = "servicio", allowableValues = {"servicio", "tienda"})
    private String sistemaType;

    @NotNull
    @Schema(description = "ID del giro comercial (restaurante, imprenta, etc.)")
    private Long idGiro;

    @NotBlank
    @Email
    @Schema(description = "Correo de contacto del negocio", example = "contacto@thejulians.com")
    private String correo;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(description = "Teléfono o WhatsApp de contacto, 10 dígitos")
    private String telefono;

    @Size(max = 200)
    @Schema(description = "Dirección física del negocio", nullable = true)
    private String direccion;

    @NotNull
    @Schema(description = "ID del plan de suscripción elegido")
    private Long idPlan;

    @NotNull(message = "El municipio es obligatorio")
    @Schema(description = "ID del municipio donde se ubica el negocio")
    private Long idMunicipio;

    // --- Datos de facturación: opcionales, pero si se captura uno de los 4 primeros deben ir los 4 ---
    @Size(max = 150)
    @Schema(description = "Razón social", nullable = true)
    private String razonSocial;

    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC no es válido")
    @Schema(description = "RFC (12 caracteres persona moral, 13 persona física)", example = "XAXX010101000", nullable = true)
    private String rfc;

    @Pattern(regexp = "^\\d{3}$", message = "El régimen fiscal es la clave de 3 dígitos del SAT (ej. 601)")
    @Schema(description = "Clave del régimen fiscal (SAT)", example = "612", nullable = true)
    private String regimenFiscal;

    @Pattern(regexp = "^\\d{5}$", message = "El código postal fiscal debe tener 5 dígitos")
    @Schema(description = "Código postal del domicilio fiscal", nullable = true)
    private String codigoPostalFiscal;

    @Email
    @Size(max = 150)
    @Schema(description = "Correo al que se envían las facturas", nullable = true)
    private String correoFacturacion;

    @Size(max = 300)
    @Schema(description = "Notas internas del superadmin (el negocio no las ve)", nullable = true)
    private String notasInternas;

    // Los opcionales llegan como "" cuando el formulario los deja vacíos: se guardan como null
    // (y el RFC en mayúsculas) para que las validaciones de formato no rechacen un campo en blanco.
    public void setRazonSocial(String valor) { this.razonSocial = vacioANull(valor); }
    public void setRfc(String valor) { String v = vacioANull(valor); this.rfc = v == null ? null : v.toUpperCase(); }
    public void setRegimenFiscal(String valor) { this.regimenFiscal = vacioANull(valor); }
    public void setCodigoPostalFiscal(String valor) { this.codigoPostalFiscal = vacioANull(valor); }
    public void setCorreoFacturacion(String valor) { this.correoFacturacion = vacioANull(valor); }
    public void setNotasInternas(String valor) { this.notasInternas = vacioANull(valor); }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
