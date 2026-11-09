package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.CategoriaResponseDTO;
import mx.com.gestishop.application.dto.response.UnidadMedidaResponseDTO;
import mx.com.gestishop.domain.model.CatUnidadMedida;
import mx.com.gestishop.domain.model.Categoria;

// Convierte entre entidades de categorías/unidades y sus DTOs
public class CategoriaMapper {

    private CategoriaMapper() {
    }

    // Respuesta de una categoría; los totales los calcula el service con consultas agrupadas
    public static CategoriaResponseDTO toResponse(Categoria c, int totalSubcategorias, int totalProductos) {
        return CategoriaResponseDTO.builder()
                .uuidCategoria(c.getUuidCategoria())
                .nombre(c.getNombre())
                .descripcion(c.getDescripcion())
                .orden(c.getOrden())
                .estatus(c.getEstatus())
                .uuidPadre(c.getPadre() != null ? c.getPadre().getUuidCategoria() : null)
                .nombrePadre(c.getPadre() != null ? c.getPadre().getNombre() : null)
                .totalSubcategorias(totalSubcategorias)
                .totalProductos(totalProductos)
                .build();
    }

    public static UnidadMedidaResponseDTO toResponse(CatUnidadMedida u) {
        return UnidadMedidaResponseDTO.builder()
                .idUnidad(u.getIdUnidad())
                .clave(u.getClave())
                .nombre(u.getNombre())
                .permiteDecimales(u.getPermiteDecimales())
                .build();
    }
}
