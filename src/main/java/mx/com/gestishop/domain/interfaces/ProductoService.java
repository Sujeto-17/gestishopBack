package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.ProductoDatosRequestDTO;
import mx.com.gestishop.application.dto.request.ProductoRequestDTO;
import mx.com.gestishop.application.dto.request.VarianteDatosRequestDTO;
import mx.com.gestishop.application.dto.request.VarianteRequestDTO;
import mx.com.gestishop.application.dto.response.ProductoListadoResponseDTO;
import mx.com.gestishop.application.dto.response.ProductoOpcionesResponseDTO;
import mx.com.gestishop.application.dto.response.ProductoResponseDTO;
import mx.com.gestishop.application.dto.response.VarianteResponseDTO;

import java.util.List;
import java.util.UUID;

// Gestión de productos de venta y sus variantes (el stock solo cambia con movimientos de inventario)
public interface ProductoService {

    // Opciones del formulario según el negocio (por ahora, si permite mayoreo)
    ProductoOpcionesResponseDTO opciones(Long idNegocio);

    // Productos del negocio con su resumen de existencias; con soloActivos=true se omiten los inactivos
    List<ProductoListadoResponseDTO> listar(Long idNegocio, boolean soloActivos);

    // Detalle con atributos y variantes
    ProductoResponseDTO obtener(UUID uuidProducto, Long idNegocio);

    // Crea el producto, sus atributos, todas sus variantes y la existencia inicial de cada una
    ProductoResponseDTO crear(ProductoRequestDTO dto, Long idNegocio, UUID uuidUsuario);

    // Edita solo los datos generales (los atributos y las variantes se editan por separado)
    ProductoResponseDTO actualizar(UUID uuidProducto, ProductoDatosRequestDTO dto, Long idNegocio);

    ProductoResponseDTO cambiarEstatus(UUID uuidProducto, Long idNegocio);

    // Elimina el producto si nunca tuvo movimientos; de lo contrario se desactiva
    void eliminar(UUID uuidProducto, Long idNegocio);

    // Agrega una variante nueva (otra combinación de valores) a un producto existente
    VarianteResponseDTO agregarVariante(UUID uuidProducto, VarianteRequestDTO dto, Long idNegocio, UUID uuidUsuario);

    // Edita SKU, código de barras, precios y stock mínimo de una variante
    VarianteResponseDTO actualizarVariante(UUID uuidProducto, UUID uuidVariante, VarianteDatosRequestDTO dto, Long idNegocio);

    VarianteResponseDTO cambiarEstatusVariante(UUID uuidProducto, UUID uuidVariante, Long idNegocio);
}
