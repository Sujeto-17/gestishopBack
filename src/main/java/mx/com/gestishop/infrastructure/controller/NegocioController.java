package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.NegocioRequestDTO;
import mx.com.gestishop.application.dto.response.NegocioResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.NegocioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// CRUD de negocios registrados en la plataforma. Exclusivo del superadmin
@Tag(name = "Negocios", description = "Alta y gestión de negocios que rentan el sistema")
@RestController
@RequestMapping("/api/v1/negocios")
@RequiredArgsConstructor
public class NegocioController extends BaseController {

    private final NegocioService negocioService;

    @Operation(summary = "Registrar un negocio nuevo",
            description = "Solo superadmin. El negocio inicia automáticamente en estatus 'prueba' con 15 días de vigencia.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> crear(@Valid @RequestBody NegocioRequestDTO dto) {
        return okCreado(negocioService.crear(dto));
    }

    @Operation(summary = "Actualizar datos de un negocio", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{uuidNegocio}")
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> actualizar(
            @PathVariable UUID uuidNegocio, @Valid @RequestBody NegocioRequestDTO dto) {
        return okActualizado(negocioService.actualizar(uuidNegocio, dto));
    }

    @Operation(summary = "Suspender o reactivar un negocio", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PatchMapping("/{uuidNegocio}/estatus")
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> cambiarEstatus(@PathVariable UUID uuidNegocio) {
        return okActualizado(negocioService.cambiarEstatus(uuidNegocio));
    }

    @Operation(summary = "Obtener un negocio por su identificador público", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/{uuidNegocio}")
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> obtener(@PathVariable UUID uuidNegocio) {
        return okEncontrado(negocioService.obtenerPorUuid(uuidNegocio));
    }

    @Operation(summary = "Listar negocios activos", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<NegocioResponseDTO>>> listarActivos() {
        return ok(negocioService.listarActivos());
    }
}
