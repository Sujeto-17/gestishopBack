package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.SuscripcionRequestDTO;
import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.SuscripcionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión del historial de suscripciones (pagos) de los negocios.
 * Exclusivo del superadmin — es quien cobra la renta del sistema.
 * El primer periodo de cada negocio se crea automáticamente al darlo
 * de alta (ver NegocioServiceImpl), no existe un POST directo aquí.
 */
@Tag(name = "Suscripciones", description = "Historial de pagos y periodos de suscripción por negocio")
@RestController
@RequestMapping("/api/v1/suscripciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuscripcionController extends BaseController {

    private final SuscripcionService suscripcionService;

    @Operation(summary = "Confirmar el pago de un periodo",
            description = "Marca un periodo pendiente como pagado, registrando fecha y método de pago.")
    @PatchMapping("/{idSuscripcion}/pagar")
    public ResponseEntity<ApiDataResponseDTO<SuscripcionResponseDTO>> confirmarPago(
            @PathVariable Long idSuscripcion, @Valid @RequestBody SuscripcionRequestDTO dto) {
        return okActualizado(suscripcionService.confirmarPago(idSuscripcion, dto));
    }

    @Operation(summary = "Renovar la suscripción de un negocio",
            description = "Genera el siguiente periodo de 30 días. Si el periodo actual aún no vence, " +
                    "el nuevo inicia justo al terminar el anterior (no se pierden días pagados).")
    @PostMapping("/negocio/{idNegocio}/renovar")
    public ResponseEntity<ApiDataResponseDTO<SuscripcionResponseDTO>> renovar(@PathVariable Long idNegocio) {
        return okCreado(suscripcionService.renovar(idNegocio));
    }

    @Operation(summary = "Listar el historial de un negocio", description = "Del periodo más reciente al más antiguo.")
    @GetMapping("/negocio/{idNegocio}")
    public ResponseEntity<ApiDataResponseDTO<List<SuscripcionResponseDTO>>> listarPorNegocio(
            @PathVariable Long idNegocio) {
        return ok(suscripcionService.listarPorNegocio(idNegocio));
    }

    @Operation(summary = "Listar periodos vencidos sin pagar",
            description = "Reporte de negocios morosos: periodos ya vencidos que nunca se marcaron como pagados.")
    @GetMapping("/vencidas")
    public ResponseEntity<ApiDataResponseDTO<List<SuscripcionResponseDTO>>> listarVencidasPendientes() {
        return ok(suscripcionService.listarVencidasPendientes());
    }
}