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

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(description = "Teléfono a 10 dígitos", nullable = true)
    private String telefono;

    @Size(max = 200)
    @Schema(description = "Dirección física del negocio", nullable = true)
    private String direccion;

    @NotNull
    @Schema(description = "ID del plan de suscripción elegido")
    private Long idPlan;
}
