package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.ProductoDatosRequestDTO;
import mx.com.gestishop.application.dto.request.ProductoRequestDTO;
import mx.com.gestishop.application.dto.request.VarianteDatosRequestDTO;
import mx.com.gestishop.application.dto.request.VarianteRequestDTO;
import mx.com.gestishop.application.dto.response.ProductoListadoResponseDTO;
import mx.com.gestishop.application.dto.response.ProductoOpcionesResponseDTO;
import mx.com.gestishop.application.dto.response.ProductoResponseDTO;
import mx.com.gestishop.application.dto.response.VarianteResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.ProductoService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Productos de venta del negocio. El negocio siempre sale del token. Ninguna respuesta incluye costos ni proveedores.
@Tag(name = "Productos", description = "Productos de venta con sus variantes, precios y existencias")
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController extends BaseController {

    private final ProductoService productoService;
    private final SesionActual sesionActual;

    // Consulta: admin y personal con el módulo (el gerente ve productos, existencias y precios, nunca costos)
    @Operation(summary = "Opciones del formulario de productos",
            description = "Indica si el negocio puede capturar precio de mayoreo (solo tipo tienda).")
    @PreAuthorize("hasAnyRole('ADMIN','TRABAJADOR') and @sesionActual.tieneModulo('productos')")
    @GetMapping("/opciones")
    public ResponseEntity<ApiDataResponseDTO<ProductoOpcionesResponseDTO>> opciones() {
        return ok(productoService.opciones(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Listar productos del negocio",
            description = "Con resumen de variantes, existencias y rango de precios. Con soloActivos=true se omiten los inactivos.")
    @PreAuthorize("hasAnyRole('ADMIN','TRABAJADOR') and @sesionActual.tieneModulo('productos')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<ProductoListadoResponseDTO>>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ok(productoService.listar(sesionActual.idNegocioObligatorio(), soloActivos));
    }

    @Operation(summary = "Obtener un producto", description = "Con sus atributos y variantes (precios y existencias).")
    @PreAuthorize("hasAnyRole('ADMIN','TRABAJADOR') and @sesionActual.tieneModulo('productos')")
    @GetMapping("/{uuidProducto}")
    public ResponseEntity<ApiDataResponseDTO<ProductoResponseDTO>> obtener(@PathVariable UUID uuidProducto) {
        return okEncontrado(productoService.obtener(uuidProducto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Crear un producto con sus variantes",
            description = "Recibe los atributos elegidos y cada variante con su combinación de valores, precios y existencia inicial. "
                    + "Sin atributos se crea una sola variante. El mayoreo solo aplica a negocios tipo tienda.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<ProductoResponseDTO>> crear(@Valid @RequestBody ProductoRequestDTO dto) {
        return okCreado(productoService.crear(dto, sesionActual.idNegocioObligatorio(), sesionActual.uuidUsuario()));
    }

    @Operation(summary = "Editar los datos generales de un producto",
            description = "No cambia atributos ni variantes. La unidad no se puede cambiar si ya hay movimientos.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PutMapping("/{uuidProducto}")
    public ResponseEntity<ApiDataResponseDTO<ProductoResponseDTO>> actualizar(
            @PathVariable UUID uuidProducto, @Valid @RequestBody ProductoDatosRequestDTO dto) {
        return okActualizado(productoService.actualizar(uuidProducto, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Activar o desactivar un producto")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PatchMapping("/{uuidProducto}/estatus")
    public ResponseEntity<ApiDataResponseDTO<ProductoResponseDTO>> cambiarEstatus(@PathVariable UUID uuidProducto) {
        return okActualizado(productoService.cambiarEstatus(uuidProducto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Eliminar un producto",
            description = "Solo si nunca tuvo movimientos ni solicitudes de compra; si los tiene, se desactiva.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @DeleteMapping("/{uuidProducto}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminar(@PathVariable UUID uuidProducto) {
        productoService.eliminar(uuidProducto, sesionActual.idNegocioObligatorio());
        return okEliminado();
    }

    @Operation(summary = "Agregar una variante a un producto con atributos",
            description = "Otra combinación de valores (por ejemplo, una talla nueva). Puede traer existencia inicial.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PostMapping("/{uuidProducto}/variantes")
    public ResponseEntity<ApiDataResponseDTO<VarianteResponseDTO>> agregarVariante(
            @PathVariable UUID uuidProducto, @Valid @RequestBody VarianteRequestDTO dto) {
        return okCreado(productoService.agregarVariante(
                uuidProducto, dto, sesionActual.idNegocioObligatorio(), sesionActual.uuidUsuario()));
    }

    @Operation(summary = "Editar una variante",
            description = "SKU, código de barras, precios (menudeo y mayoreo) y stock mínimo. El stock real cambia solo con movimientos.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PutMapping("/{uuidProducto}/variantes/{uuidVariante}")
    public ResponseEntity<ApiDataResponseDTO<VarianteResponseDTO>> actualizarVariante(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidVariante,
            @Valid @RequestBody VarianteDatosRequestDTO dto) {
        return okActualizado(productoService.actualizarVariante(
                uuidProducto, uuidVariante, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Activar o desactivar una variante")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PatchMapping("/{uuidProducto}/variantes/{uuidVariante}/estatus")
    public ResponseEntity<ApiDataResponseDTO<VarianteResponseDTO>> cambiarEstatusVariante(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidVariante) {
        return okActualizado(productoService.cambiarEstatusVariante(
                uuidProducto, uuidVariante, sesionActual.idNegocioObligatorio()));
    }
}