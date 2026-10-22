package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos para crear o actualizar un módulo del sistema
@Getter
@Setter
@Schema(name = "CatModuloRequestDTO", description = "Datos de un módulo funcional del sistema")
public class CatModuloRequestDTO {

    // La clave identifica al módulo en el código. Se define al CREAR y después no se puede cambiar, aunque se
    // renombre el módulo. Si no se envía, se genera a partir del nombre ("Categorías" -> "categorias").
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,39}$", message = "La clave va en minúsculas, sin acentos ni espacios (ej. cotizaciones)")
    @Schema(description = "Clave estable del módulo (solo al crear)", example = "cotizaciones", nullable = true)
    private String clave;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60)
    @Schema(description = "Nombre visible del módulo", example = "Pedidos")
    private String nombre;

    @Schema(description = "Descripción breve del módulo", example = "Gestión de pedidos con seguimiento", nullable = true)
    private String descripcion;

    @Schema(description = "Ícono FontAwesome para el sidebar", example = "clipboard-list", nullable = true)
    private String icono;

    @Schema(description = "Ruta base en el frontend Angular", example = "/pedidos", nullable = true)
    private String ruta;

    @Schema(description = "Orden de aparición en el menú", example = "3")
    private Short orden;

    // Un campo vacío del formulario llega como "": se trata como "no enviada" (se generará del nombre)
    public void setClave(String clave) {
        this.clave = clave == null || clave.isBlank() ? null : clave.trim();
    }
}
