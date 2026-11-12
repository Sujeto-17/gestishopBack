package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AjusteInventarioRequestDTO;
import mx.com.gestishop.application.dto.request.CostoMovimientoRequestDTO;
import mx.com.gestishop.application.dto.request.ResurtidoRequestDTO;
import mx.com.gestishop.application.dto.request.SalidaInventarioRequestDTO;
import mx.com.gestishop.application.dto.response.InventarioOpcionesResponseDTO;
import mx.com.gestishop.application.dto.response.MovimientoResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.MovimientoInventarioService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Movimientos de inventario. Resurtir y registrar mermas lo puede hacer el admin y el gerente; los costos y ajustes, solo el admin.
@Tag(name = "Movimientos de inventario", description = "Resurtidos, mermas, ajustes e historial de existencias")
@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class MovimientoInventarioController extends BaseController {

    private final MovimientoInventarioService movimientoService;
    private final SesionActual sesionActual;

    @Operation(summary = "Opciones para registrar movimientos",
            description = "Variantes activas con su existencia y proveedores activos (solo nombre). Sin costos.")
    @PreAuthorize("(hasRole('ADMIN') or @sesionActual.esGerente()) and @sesionActual.tieneModulo('productos')")
    @GetMapping("/opciones")
    public ResponseEntity<ApiDataResponseDTO<InventarioOpcionesResponseDTO>> opciones() {
        return ok(movimientoService.opciones(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Resurtir una variante",
            description = "Registra la mercancía que llegó de un proveedor: la cantidad se SUMA a la existencia. "
                    + "El costo solo lo captura el admin; si no se indica, queda como resurtido pendiente de costo.")
    @PreAuthorize("(hasRole('ADMIN') or @sesionActual.esGerente()) and @sesionActual.tieneModulo('productos')")
    @PostMapping("/resurtidos")
    public ResponseEntity<ApiDataResponseDTO<MovimientoResponseDTO>> resurtir(@Valid @RequestBody ResurtidoRequestDTO dto) {
        return okCreado(movimientoService.resurtir(dto, sesionActual.idNegocioObligatorio(),
                sesionActual.uuidUsuario(), sesionActual.esAdmin()));
    }

    @Operation(summary = "Registrar una merma o consumo",
            description = "Resta de la existencia. Exige explicar qué pasó y no puede dejar la existencia en negativo.")
    @PreAuthorize("(hasRole('ADMIN') or @sesionActual.esGerente()) and @sesionActual.tieneModulo('productos')")
    @PostMapping("/salidas")
    public ResponseEntity<ApiDataResponseDTO<MovimientoResponseDTO>> registrarSalida(@Valid @RequestBody SalidaInventarioRequestDTO dto) {
        return okCreado(movimientoService.registrarSalida(dto, sesionActual.idNegocioObligatorio(), sesionActual.uuidUsuario()));
    }

    @Operation(summary = "Ajustar la existencia por conteo físico",
            description = "Se indica la existencia real y el sistema registra la diferencia (sube o baja). Solo admin.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PostMapping("/ajustes")
    public ResponseEntity<ApiDataResponseDTO<MovimientoResponseDTO>> ajustar(@Valid @RequestBody AjusteInventarioRequestDTO dto) {
        return okCreado(movimientoService.ajustar(dto, sesionActual.idNegocioObligatorio(), sesionActual.uuidUsuario()));
    }

    @Operation(summary = "Historial de movimientos",
            description = "Del más reciente al más antiguo. Filtros opcionales: variante, tipo (entrada|salida|ajuste), motivo y rango de fechas (desde y hasta, ambos incluidos, yyyy-MM-dd). "
                    + "El gerente solo ve compras, mermas y consumos, sin costos.")
    @PreAuthorize("(hasRole('ADMIN') or @sesionActual.esGerente()) and @sesionActual.tieneModulo('productos')")
    @GetMapping("/movimientos")
    public ResponseEntity<ApiDataResponseDTO<List<MovimientoResponseDTO>>> listar(
            @RequestParam(required = false) UUID uuidVariante,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "50") int limite) {
        return ok(movimientoService.listar(uuidVariante, tipo, motivo, desde, hasta, limite,
                sesionActual.idNegocioObligatorio(), sesionActual.esAdmin()));
    }

    @Operation(summary = "Resurtidos pendientes de costo",
            description = "Entradas por compra que el gerente registró sin costo y que el admin debe completar.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @GetMapping("/resurtidos/pendientes-costo")
    public ResponseEntity<ApiDataResponseDTO<List<MovimientoResponseDTO>>> pendientesDeCosto() {
        return ok(movimientoService.pendientesDeCosto(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Completar el costo de un resurtido",
            description = "Se puede hacer una sola vez. También actualiza el costo de la variante con ese proveedor (queda en el historial) "
                    + "sin cambiar cuál es el preferido.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PatchMapping("/movimientos/{uuidMovimiento}/costo")
    public ResponseEntity<ApiDataResponseDTO<MovimientoResponseDTO>> completarCosto(
            @PathVariable UUID uuidMovimiento, @Valid @RequestBody CostoMovimientoRequestDTO dto) {
        return okActualizado(movimientoService.completarCosto(uuidMovimiento, dto, sesionActual.idNegocioObligatorio()));
    }
}
