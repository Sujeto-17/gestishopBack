package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos para crear o actualizar un giro de negocio del catálogo global
@Getter
@Setter
@Schema(name = "CatGiroNegocioRequestDTO", description = "Datos de un giro comercial (restaurante, imprenta, barbería, etc.)")
public class CatGiroNegocioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60)
    @Schema(description = "Nombre visible del giro", example = "Restaurantes / comida")
    private String nombre;

    @Schema(description = "Nombre de ícono FontAwesome, opcional, solo visual", example = "utensils", nullable = true)
    private String icono;

    @Schema(description = "Orden de aparición en selects", example = "1")
    private Short orden;
}
