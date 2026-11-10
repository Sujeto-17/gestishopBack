package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.VarianteProveedorRequestDTO;
import mx.com.gestishop.application.dto.response.HistorialCostoResponseDTO;
import mx.com.gestishop.application.dto.response.VarianteCostosResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.VarianteCostoService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Costos de los productos por proveedor. Solo admin: el gerente nunca los ve. El negocio sale del token.
@Tag(name = "Costos de productos", description = "Proveedores y costos por variante, con ganancia e historial")
@RestController
@RequestMapping("/api/v1/productos/{uuidProducto}/costos")
@RequiredArgsConstructor
public class ProductoCostoController extends BaseController {

    private final VarianteCostoService costoService;
    private final SesionActual sesionActual;

    @Operation(summary = "Costos del producto",
            description = "Por cada variante: precio de venta, proveedores con su costo, costo del preferido y ganancia.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<VarianteCostosResponseDTO>>> listar(@PathVariable UUID uuidProducto) {
        return ok(costoService.listar(uuidProducto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Registrar o actualizar el costo de una variante con un proveedor",
            description = "Si el proveedor ya surtía la variante se actualiza su costo (queda en el historial). "
                    + "El primer proveedor de una variante es siempre el preferido.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PutMapping("/variantes/{uuidVariante}")
    public ResponseEntity<ApiDataResponseDTO<VarianteCostosResponseDTO>> guardar(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidVariante,
            @Valid @RequestBody VarianteProveedorRequestDTO dto) {
        return okActualizado(costoService.guardar(uuidProducto, uuidVariante, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Mismo proveedor y costo para todas las variantes",
            description = "Atajo para cuando todas las variantes cuestan lo mismo (por ejemplo, todas las tallas de una playera).")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PutMapping("/todas")
    public ResponseEntity<ApiDataResponseDTO<List<VarianteCostosResponseDTO>>> guardarEnTodas(
            @PathVariable UUID uuidProducto, @Valid @RequestBody VarianteProveedorRequestDTO dto) {
        return okActualizado(costoService.guardarEnTodas(uuidProducto, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Marcar el proveedor preferido de una variante")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PatchMapping("/variantes/{uuidVariante}/proveedores/{uuidProveedor}/preferido")
    public ResponseEntity<ApiDataResponseDTO<VarianteCostosResponseDTO>> marcarPreferido(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidVariante, @PathVariable UUID uuidProveedor) {
        return okActualizado(costoService.marcarPreferido(
                uuidProducto, uuidVariante, uuidProveedor, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Quitar un proveedor de una variante",
            description = "Si era el preferido, pasa a serlo el de menor costo. El historial se conserva.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @DeleteMapping("/variantes/{uuidVariante}/proveedores/{uuidProveedor}")
    public ResponseEntity<ApiDataResponseDTO<VarianteCostosResponseDTO>> quitar(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidVariante, @PathVariable UUID uuidProveedor) {
        return okActualizado(costoService.quitar(
                uuidProducto, uuidVariante, uuidProveedor, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Historial de costos de una variante", description = "Últimos 50 cambios, del más reciente al más antiguo.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @GetMapping("/variantes/{uuidVariante}/historial")
    public ResponseEntity<ApiDataResponseDTO<List<HistorialCostoResponseDTO>>> historial(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidVariante) {
        return ok(costoService.historial(uuidProducto, uuidVariante, sesionActual.idNegocioObligatorio()));
    }
}
