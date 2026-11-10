package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Datos generales de un producto (los que se pueden editar después de crearlo)
@Getter
@Setter
@Schema(name = "ProductoDatosRequestDTO", description = "Datos generales del producto")
public class ProductoDatosRequestDTO {

    @NotNull
    @Schema(description = "Categoría del producto (familia o subfamilia del negocio)")
    private UUID uuidCategoria;

    @NotBlank
    @Size(max = 120)
    @Schema(example = "Taza sublimable")
    private String nombre;

    @Size(max = 60)
    private String marca;

    @Size(max = 60)
    private String modelo;

    @Size(max = 500)
    private String descripcion;

    @NotNull
    @Schema(description = "Unidad de medida (catálogo de unidades)")
    private Long idUnidad;

    @NotNull
    @Schema(description = "Tasa de IVA del producto: 0, 8 o 16", example = "16")
    private Short tasaIva;

    // Los textos vacíos se guardan como null
    public void setMarca(String marca) {
        this.marca = marca == null || marca.isBlank() ? null : marca.trim();
    }

    public void setModelo(String modelo) {
        this.modelo = modelo == null || modelo.isBlank() ? null : modelo.trim();
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }
}
