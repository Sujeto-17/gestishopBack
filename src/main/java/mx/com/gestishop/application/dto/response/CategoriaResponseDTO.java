package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(name = "CategoriaResponseDTO")
public class CategoriaResponseDTO {

    private UUID uuidCategoria;
    private String nombre;
    private String descripcion;
    private Short orden;
    private String estatus;

    @Schema(description = "Familia a la que pertenece; null si es una familia")
    private UUID uuidPadre;
    private String nombrePadre;

    @Schema(description = "Cantidad de subfamilias (solo familias)")
    private int totalSubcategorias;

    @Schema(description = "Cantidad de productos asignados directamente a esta categoría")
    private int totalProductos;
}
