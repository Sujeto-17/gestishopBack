package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.ProveedorRequestDTO;
import mx.com.gestishop.application.dto.response.ProveedorResponseDTO;

import java.util.List;
import java.util.UUID;

// Gestión de los proveedores de un negocio, con sus contactos y descuentos por volumen
public interface ProveedorService {

    // Proveedores del negocio; con soloActivos=true se omiten los inactivos (para selectores)
    List<ProveedorResponseDTO> listar(Long idNegocio, boolean soloActivos);

    ProveedorResponseDTO obtener(UUID uuidProveedor, Long idNegocio);

    ProveedorResponseDTO crear(ProveedorRequestDTO dto, Long idNegocio);

    // Reemplaza los datos del proveedor, sus contactos y sus descuentos por los recibidos
    ProveedorResponseDTO actualizar(UUID uuidProveedor, ProveedorRequestDTO dto, Long idNegocio);

    // Activa o desactiva el proveedor
    ProveedorResponseDTO cambiarEstatus(UUID uuidProveedor, Long idNegocio);

    // Solo si nunca se ha usado (sin costos, solicitudes ni entradas); de lo contrario se debe desactivar
    void eliminar(UUID uuidProveedor, Long idNegocio);
}
