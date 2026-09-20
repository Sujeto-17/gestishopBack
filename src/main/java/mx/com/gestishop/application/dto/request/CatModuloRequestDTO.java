package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Datos para crear o actualizar un módulo del sistema
@Getter
@Setter
@Schema(name = "CatModuloRequestDTO", description = "Datos de un módulo funcional del sistema")
public class CatModuloRequestDTO {

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
}
