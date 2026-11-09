package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.ProveedorRequestDTO;
import mx.com.gestishop.application.dto.response.ProveedorResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.ProveedorService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Proveedores del negocio. Solo el admin: el gerente nunca los ve. El negocio siempre sale del token.
@Tag(name = "Proveedores", description = "Proveedores del negocio, con contactos y descuentos por volumen")
@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
public class ProveedorController extends BaseController {

    private final ProveedorService proveedorService;
    private final SesionActual sesionActual;

    // Para elegir proveedor al registrar costos de un producto basta con el módulo de productos
    @Operation(summary = "Listar proveedores del negocio",
            description = "Con soloActivos=true se omiten los inactivos (para selectores). La lista no incluye los descuentos.")
    @PreAuthorize("hasRole('ADMIN') and (@sesionActual.tieneModulo('proveedores') or @sesionActual.tieneModulo('productos'))")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<ProveedorResponseDTO>>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ok(proveedorService.listar(sesionActual.idNegocioObligatorio(), soloActivos));
    }

    @Operation(summary = "Obtener un proveedor", description = "Incluye contactos y descuentos por volumen.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('proveedores')")
    @GetMapping("/{uuidProveedor}")
    public ResponseEntity<ApiDataResponseDTO<ProveedorResponseDTO>> obtener(@PathVariable UUID uuidProveedor) {
        return okEncontrado(proveedorService.obtener(uuidProveedor, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Crear un proveedor", description = "Con sus contactos y descuentos en una sola petición.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('proveedores')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<ProveedorResponseDTO>> crear(@Valid @RequestBody ProveedorRequestDTO dto) {
        return okCreado(proveedorService.crear(dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Editar un proveedor",
            description = "Reemplaza sus datos, contactos y descuentos por los enviados: lo que no venga se elimina.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('proveedores')")
    @PutMapping("/{uuidProveedor}")
    public ResponseEntity<ApiDataResponseDTO<ProveedorResponseDTO>> actualizar(
            @PathVariable UUID uuidProveedor, @Valid @RequestBody ProveedorRequestDTO dto) {
        return okActualizado(proveedorService.actualizar(uuidProveedor, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Activar o desactivar un proveedor")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('proveedores')")
    @PatchMapping("/{uuidProveedor}/estatus")
    public ResponseEntity<ApiDataResponseDTO<ProveedorResponseDTO>> cambiarEstatus(@PathVariable UUID uuidProveedor) {
        return okActualizado(proveedorService.cambiarEstatus(uuidProveedor, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Eliminar un proveedor",
            description = "Solo si nunca se ha usado (costos, solicitudes de compra o entradas); si no, se desactiva.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('proveedores')")
    @DeleteMapping("/{uuidProveedor}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminar(@PathVariable UUID uuidProveedor) {
        proveedorService.eliminar(uuidProveedor, sesionActual.idNegocioObligatorio());
        return okEliminado();
    }
}
