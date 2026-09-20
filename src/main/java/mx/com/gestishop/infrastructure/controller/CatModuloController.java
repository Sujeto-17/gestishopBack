package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CatModuloRequestDTO;
import mx.com.gestishop.application.dto.response.ModuloResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.CatModuloService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CRUD del catálogo de módulos del sistema. Solo el superadmin lo administra
@Tag(name = "Módulos", description = "Catálogo de módulos funcionales del sistema")
@RestController
@RequestMapping("/api/v1/modulos")
@RequiredArgsConstructor
public class CatModuloController extends BaseController {

    private final CatModuloService catModuloService;

    @Operation(summary = "Registrar un módulo nuevo", description = "Solo superadmin. No implementa la funcionalidad del módulo, solo lo hace disponible en planes y permisos.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<ModuloResponseDTO>> crear(@Valid @RequestBody CatModuloRequestDTO dto) {
        return okCreado(catModuloService.crear(dto));
    }

    @Operation(summary = "Actualizar un módulo", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{idModulo}")
    public ResponseEntity<ApiDataResponseDTO<ModuloResponseDTO>> actualizar(
            @PathVariable Long idModulo, @Valid @RequestBody CatModuloRequestDTO dto) {
        return okActualizado(catModuloService.actualizar(idModulo, dto));
    }

    @Operation(summary = "Activar o desactivar un módulo", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PatchMapping("/{idModulo}/estatus")
    public ResponseEntity<ApiDataResponseDTO<ModuloResponseDTO>> cambiarEstatus(@PathVariable Long idModulo) {
        return okActualizado(catModuloService.cambiarEstatus(idModulo));
    }

    @Operation(summary = "Listar todos los módulos", description = "Solo superadmin, para el CRUD del catálogo.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<ModuloResponseDTO>>> listarTodos() {
        return ok(catModuloService.listarTodos());
    }

    @Operation(summary = "Listar módulos activos", description = "Accesible por cualquier usuario autenticado.")
    @GetMapping("/activos")
    public ResponseEntity<ApiDataResponseDTO<List<ModuloResponseDTO>>> listarActivos() {
        return ok(catModuloService.listarActivos());
    }
}
