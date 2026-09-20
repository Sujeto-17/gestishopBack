package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CatGiroNegocioRequestDTO;
import mx.com.gestishop.application.dto.response.CatGiroNegocioResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.CatGiroNegocioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD del catálogo de giros de negocio (restaurante, imprenta, barbería...).
 * Solo el superadmin administra este catálogo; cualquier usuario autenticado
 * puede consultar la lista activa para poblar el select del alta de negocio.
 */
@Tag(name = "Giros de negocio", description = "Catálogo de giros comerciales")
@RestController
@RequestMapping("/api/v1/giros-negocio")
@RequiredArgsConstructor
public class CatGiroNegocioController extends BaseController {

    private final CatGiroNegocioService catGiroNegocioService;

    @Operation(summary = "Registrar un giro de negocio", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<CatGiroNegocioResponseDTO>> crear(
            @Valid @RequestBody CatGiroNegocioRequestDTO dto) {
        return okCreado(catGiroNegocioService.crear(dto));
    }

    @Operation(summary = "Actualizar un giro de negocio", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{idGiro}")
    public ResponseEntity<ApiDataResponseDTO<CatGiroNegocioResponseDTO>> actualizar(
            @PathVariable Long idGiro, @Valid @RequestBody CatGiroNegocioRequestDTO dto) {
        return okActualizado(catGiroNegocioService.actualizar(idGiro, dto));
    }

    @Operation(summary = "Activar o desactivar un giro", description = "Solo superadmin.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PatchMapping("/{idGiro}/estatus")
    public ResponseEntity<ApiDataResponseDTO<CatGiroNegocioResponseDTO>> cambiarEstatus(@PathVariable Long idGiro) {
        return okActualizado(catGiroNegocioService.cambiarEstatus(idGiro));
    }

    @Operation(summary = "Listar todos los giros", description = "Solo superadmin, para el CRUD del catálogo.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<CatGiroNegocioResponseDTO>>> listarTodos() {
        return ok(catGiroNegocioService.listarTodos());
    }

    @Operation(summary = "Listar giros activos", description = "Accesible por cualquier usuario autenticado, para poblar selects.")
    @GetMapping("/activos")
    public ResponseEntity<ApiDataResponseDTO<List<CatGiroNegocioResponseDTO>>> listarActivos() {
        return ok(catGiroNegocioService.listarActivos());
    }
}
