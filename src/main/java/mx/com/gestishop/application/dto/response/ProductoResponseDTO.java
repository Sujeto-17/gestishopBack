package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

// Detalle de un producto: datos generales, atributos y variantes
@Getter
@Builder
@Schema(name = "ProductoResponseDTO")
public class ProductoResponseDTO {

    private UUID uuidProducto;
    private String nombre;
    private String marca;
    private String modelo;
    private String descripcion;
    private UUID uuidCategoria;
    private String categoria;
    private Long idUnidad;
    private String unidad;
    private boolean unidadPermiteDecimales;
    private Short tasaIva;
    private String estatus;
    private List<ProductoAtributoResponseDTO> atributos;
    private List<VarianteResponseDTO> variantes;
}