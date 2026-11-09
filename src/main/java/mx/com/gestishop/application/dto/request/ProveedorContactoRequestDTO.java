package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Persona de contacto de un proveedor; debe traer al menos teléfono, WhatsApp o correo
@Getter
@Setter
@Schema(name = "ProveedorContactoRequestDTO", description = "Contacto de un proveedor")
public class ProveedorContactoRequestDTO {

    @NotBlank(message = "El nombre del contacto es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 60)
    @Schema(example = "Vendedor", nullable = true)
    private String puesto;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(nullable = true)
    private String telefono;

    @Pattern(regexp = "^\\d{10}$", message = "El WhatsApp debe tener 10 dígitos")
    @Schema(nullable = true)
    private String whatsapp;

    @Email
    @Size(max = 150)
    @Schema(nullable = true)
    private String correo;

    @Schema(description = "Solo un contacto por proveedor puede ser el principal", defaultValue = "false")
    private Boolean esPrincipal = false;

    @Size(max = 200)
    @Schema(nullable = true)
    private String notas;

    // Los opcionales llegan como "" cuando el formulario los deja vacíos: se guardan como null
    public void setNombre(String valor) { this.nombre = vacioANull(valor); }
    public void setPuesto(String valor) { this.puesto = vacioANull(valor); }
    public void setTelefono(String valor) { this.telefono = vacioANull(valor); }
    public void setWhatsapp(String valor) { this.whatsapp = vacioANull(valor); }
    public void setCorreo(String valor) { this.correo = vacioANull(valor); }
    public void setNotas(String valor) { this.notas = vacioANull(valor); }
    public void setEsPrincipal(Boolean valor) { this.esPrincipal = valor != null && valor; }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}