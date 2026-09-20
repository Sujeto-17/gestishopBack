package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.PlanRequestDTO;
import mx.com.gestishop.application.dto.response.PlanResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// CRUD de planes de suscripción. Solo el superadmin los administra
@Tag(name = "Planes", description = "Catálogo de planes de suscripción")
@RestController
@RequestMapping("/api/v1/planes")
@RequiredArgsConstructor
public class PlanController extends BaseController {

    private final PlanService planService;

    @Operation(summary = "Registrar un plan nuevo", description = "Solo superadmin. Recibe los IDs de los módulos incluidos.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<PlanResponseDTO>> crear(@Valid @RequestBody PlanRequestDTO dto) {
        return okCreado(planService.crear(dto));
    }

    @Operation(summary = "Actualizar un plan", description = "Solo superadmin. La lista de módulos se reemplaza por completo.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{uuidPlan}")
    public ResponseEntity<ApiDataResponseDTO<PlanResponseDTO>> actualizar(
            @PathVariable UUID uuidPlan, @Valid @RequestBody PlanRequestDTO dto) {
        return okActualizado(planService.actualizar(uuidPlan, dto));
    }

    @Operation(summary = "Activar o desactivar un plan", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PatchMapping("/{uuidPlan}/estatus")
    public ResponseEntity<ApiDataResponseDTO<PlanResponseDTO>> cambiarEstatus(@PathVariable UUID uuidPlan) {
        return okActualizado(planService.cambiarEstatus(uuidPlan));
    }

    @Operation(summary = "Obtener un plan por su identificador público", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/{uuidPlan}")
    public ResponseEntity<ApiDataResponseDTO<PlanResponseDTO>> obtener(@PathVariable UUID uuidPlan) {
        return okEncontrado(planService.obtenerPorUuid(uuidPlan));
    }

    @Operation(summary = "Listar todos los planes", description = "Solo superadmin, para el CRUD del catálogo.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<PlanResponseDTO>>> listarTodos() {
        return ok(planService.listarTodos());
    }

    @Operation(summary = "Listar planes activos por tipo de sistema",
            description = "Accesible por cualquier usuario autenticado; se usa al armar el selector de plan en el alta de un negocio.")
    @GetMapping("/activos")
    public ResponseEntity<ApiDataResponseDTO<List<PlanResponseDTO>>> listarActivosPorSistema(
            @RequestParam String sistemaType) {
        return ok(planService.listarActivosPorSistema(sistemaType));
    }
}
