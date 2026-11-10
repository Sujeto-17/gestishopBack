package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.VarianteProveedorRequestDTO;
import mx.com.gestishop.application.dto.response.HistorialCostoResponseDTO;
import mx.com.gestishop.application.dto.response.VarianteCostosResponseDTO;

import java.util.List;
import java.util.UUID;

// Costos de las variantes por proveedor. Solo el admin los consulta y edita.
public interface VarianteCostoService {

    // Variantes del producto con su precio, sus proveedores y la ganancia
    List<VarianteCostosResponseDTO> listar(UUID uuidProducto, Long idNegocio);

    // Registra o actualiza el costo de una variante con un proveedor
    VarianteCostosResponseDTO guardar(UUID uuidProducto, UUID uuidVariante, VarianteProveedorRequestDTO dto, Long idNegocio);

    // Aplica el mismo proveedor y costo a todas las variantes del producto
    List<VarianteCostosResponseDTO> guardarEnTodas(UUID uuidProducto, VarianteProveedorRequestDTO dto, Long idNegocio);

    // Marca al proveedor como el preferido de la variante
    VarianteCostosResponseDTO marcarPreferido(UUID uuidProducto, UUID uuidVariante, UUID uuidProveedor, Long idNegocio);

    // Quita al proveedor de la variante; si era el preferido, pasa a serlo el de menor costo
    VarianteCostosResponseDTO quitar(UUID uuidProducto, UUID uuidVariante, UUID uuidProveedor, Long idNegocio);

    // Últimos cambios de costo de la variante
    List<HistorialCostoResponseDTO> historial(UUID uuidProducto, UUID uuidVariante, Long idNegocio);
}
