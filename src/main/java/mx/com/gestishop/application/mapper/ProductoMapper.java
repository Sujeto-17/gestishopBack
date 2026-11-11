package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.*;
import mx.com.gestishop.domain.model.Producto;
import mx.com.gestishop.domain.model.Variante;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

// Convierte entre entidades de productos/variantes y sus DTOs
public class ProductoMapper {

    private ProductoMapper() {
    }

    // Fila del listado; el resumen sale de una consulta agrupada (null si el producto no tiene variantes activas)
    public static ProductoListadoResponseDTO toListado(Producto p, Object[] resumen, String fotoPrincipal) {
        return ProductoListadoResponseDTO.builder()
                .uuidProducto(p.getUuidProducto())
                .nombre(p.getNombre())
                .marca(p.getMarca())
                .modelo(p.getModelo())
                .uuidCategoria(p.getCategoria().getUuidCategoria())
                .categoria(p.getCategoria().getNombre())
                .unidad(p.getUnidad().getClave())
                .tasaIva(p.getTasaIva())
                .estatus(p.getEstatus())
                .totalVariantes(resumen == null ? 0 : ((Number) resumen[1]).intValue())
                .stockTotal(resumen == null ? BigDecimal.ZERO : (BigDecimal) resumen[2])
                .precioDesde(resumen == null ? null : (BigDecimal) resumen[3])
                .precioHasta(resumen == null ? null : (BigDecimal) resumen[4])
                .fotoPrincipal(fotoPrincipal)
                .variantesSinExistencia(resumen == null ? 0 : ((Number) resumen[5]).intValue())
                .variantesStockBajo(resumen == null ? 0 : ((Number) resumen[6]).intValue())
                .build();
    }

    public static ProductoResponseDTO toResponse(Producto p, List<ProductoAtributoResponseDTO> atributos,
                                                 List<VarianteResponseDTO> variantes) {
        return ProductoResponseDTO.builder()
                .uuidProducto(p.getUuidProducto())
                .nombre(p.getNombre())
                .marca(p.getMarca())
                .modelo(p.getModelo())
                .descripcion(p.getDescripcion())
                .uuidCategoria(p.getCategoria().getUuidCategoria())
                .categoria(p.getCategoria().getNombre())
                .idUnidad(p.getUnidad().getIdUnidad())
                .unidad(p.getUnidad().getClave())
                .unidadPermiteDecimales(Boolean.TRUE.equals(p.getUnidad().getPermiteDecimales()))
                .tasaIva(p.getTasaIva())
                .estatus(p.getEstatus())
                .atributos(atributos)
                .variantes(variantes)
                .build();
    }

    // Variante con sus valores; la etiqueta une los valores en el orden de los atributos ("Rojo / M")
    public static VarianteResponseDTO toResponse(Variante v, List<VarianteValorResponseDTO> valores) {
        return VarianteResponseDTO.builder()
                .uuidVariante(v.getUuidVariante())
                .etiqueta(valores.stream().map(VarianteValorResponseDTO::getValor).collect(Collectors.joining(" / ")))
                .valores(valores)
                .sku(v.getSku())
                .codigoBarras(v.getCodigoBarras())
                .precioPublico(v.getPrecioPublico())
                .precioMayoreo(v.getPrecioMayoreo())
                .cantidadMayoreo(v.getCantidadMayoreo())
                .stockActual(v.getStockActual())
                .stockMinimo(v.getStockMinimo())
                .estatus(v.getEstatus())
                .build();
    }
}
