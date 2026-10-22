package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CancelarPeriodoRequestDTO;
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
 * Pagos y periodos de suscripción de los negocios.
 * Exclusivo del superadmin: es quien cobra la renta del sistema.
 * El periodo de prueba se crea solo al dar de alta el negocio (ver NegocioServiceImpl); aquí solo se
 * registran pagos, se cancelan los registrados por error y se consulta el historial.
 */
@Tag(name = "Suscripciones", description = "Pagos y periodos de suscripción por negocio")
@RestController
@RequestMapping("/api/v1/suscripciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuscripcionController extends BaseController {

    private final SuscripcionService suscripcionService;

    @Operation(summary = "Registrar el pago de un negocio y reactivar su acceso",
            description = "Acción principal cuando un cliente paga. Crea el periodo de 30 días ya pagado; si al negocio aún le " +
                    "quedaban días, empieza donde termina el actual. El negocio queda 'activo' hasta el fin de ese periodo.")
    @PostMapping("/negocio/{idNegocio}/pago")
    public ResponseEntity<ApiDataResponseDTO<SuscripcionResponseDTO>> registrarPago(
            @PathVariable Long idNegocio, @Valid @RequestBody SuscripcionRequestDTO dto) {
        return okCreado(suscripcionService.registrarPago(idNegocio, dto));
    }

    @Operation(summary = "Cancelar un periodo pagado",
            description = "Para pagos registrados por error. Recalcula el vencimiento y el estatus del negocio con los periodos " +
                    "que quedan. Solo aplica a periodos pagados.")
    @PatchMapping("/{idSuscripcion}/cancelar")
    public ResponseEntity<ApiDataResponseDTO<SuscripcionResponseDTO>> cancelarPeriodo(
            @PathVariable Long idSuscripcion, @Valid @RequestBody CancelarPeriodoRequestDTO dto) {
        return okActualizado(suscripcionService.cancelarPeriodo(idSuscripcion, dto));
    }

    @Operation(summary = "Listar el historial de un negocio", description = "Del periodo más reciente al más antiguo.")
    @GetMapping("/negocio/{idNegocio}")
    public ResponseEntity<ApiDataResponseDTO<List<SuscripcionResponseDTO>>> listarPorNegocio(
            @PathVariable Long idNegocio) {
        return ok(suscripcionService.listarPorNegocio(idNegocio));
    }
}
