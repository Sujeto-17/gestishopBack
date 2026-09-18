package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * Representa un módulo del sistema con su información de navegación.
 * Se usa cuando el superadmin administra el catálogo de módulos, o cuando el
 * frontend arma el sidebar con ícono y ruta (a diferencia de CatalogoRefDTO,
 * que solo trae id/nombre para selects simples).
 */
@Getter
@Builder
@Schema(name = "ModuloResponseDTO", description = "Módulo del sistema con datos de navegación")
public class ModuloResponseDTO {

    @Schema(description = "Identificador del módulo")
    private Long idModulo;

    @Schema(description = "Nombre visible del módulo", example = "Pedidos")
    private String nombre;

    @Schema(description = "Descripción breve de para qué sirve el módulo")
    private String descripcion;

    @Schema(description = "Nombre del ícono FontAwesome para el sidebar", example = "clipboard-list")
    private String icono;

    @Schema(description = "Ruta base del módulo en el frontend Angular", example = "/pedidos")
    private String ruta;

    @Schema(description = "Orden de aparición en el menú")
    private Short orden;

    @Schema(description = "Estatus del módulo", example = "activo")
    private String estatus;
}