package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AtributoRequestDTO;
import mx.com.gestishop.application.dto.request.AtributoValorRequestDTO;
import mx.com.gestishop.application.dto.response.AtributoResponseDTO;
import mx.com.gestishop.application.dto.response.AtributoValorResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.AtributoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Administración del catálogo global de atributos y sus valores del sistema (solo superadmin)
@Tag(name = "Atributos globales", description = "Catálogo de atributos y valores del sistema")
@RestController
@RequestMapping("/api/v1/atributos-globales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AtributoGlobalController extends BaseController {

    private final AtributoService atributoService;

    @Operation(summary = "Listar atributos con sus valores del sistema (activos e inactivos)")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<AtributoResponseDTO>>> listar() {
        return ok(atributoService.listarGlobales());
    }

    @Operation(summary = "Crear un atributo")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<AtributoResponseDTO>> crear(@Valid @RequestBody AtributoRequestDTO dto) {
        return okCreado(atributoService.crearAtributo(dto));
    }

    @Operation(summary = "Editar un atributo")
    @PutMapping("/{uuidAtributo}")
    public ResponseEntity<ApiDataResponseDTO<AtributoResponseDTO>> actualizar(
            @PathVariable UUID uuidAtributo, @Valid @RequestBody AtributoRequestDTO dto) {
        return okActualizado(atributoService.actualizarAtributo(uuidAtributo, dto));
    }

    @Operation(summary = "Activar o desactivar un atributo")
    @PatchMapping("/{uuidAtributo}/estatus")
    public ResponseEntity<ApiDataResponseDTO<AtributoResponseDTO>> cambiarEstatus(@PathVariable UUID uuidAtributo) {
        return okActualizado(atributoService.cambiarEstatusAtributo(uuidAtributo));
    }

    @Operation(summary = "Eliminar un atributo", description = "Si algún producto lo usa, se desactiva.")
    @DeleteMapping("/{uuidAtributo}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminar(@PathVariable UUID uuidAtributo) {
        atributoService.eliminarAtributo(uuidAtributo);
        return okEliminado();
    }

    @Operation(summary = "Agregar un valor del sistema a un atributo")
    @PostMapping("/{uuidAtributo}/valores")
    public ResponseEntity<ApiDataResponseDTO<AtributoValorResponseDTO>> crearValor(
            @PathVariable UUID uuidAtributo, @Valid @RequestBody AtributoValorRequestDTO dto) {
        return okCreado(atributoService.crearValorGlobal(uuidAtributo, dto));
    }

    @Operation(summary = "Editar un valor del sistema")
    @PutMapping("/valores/{uuidValor}")
    public ResponseEntity<ApiDataResponseDTO<AtributoValorResponseDTO>> actualizarValor(
            @PathVariable UUID uuidValor, @Valid @RequestBody AtributoValorRequestDTO dto) {
        return okActualizado(atributoService.actualizarValorGlobal(uuidValor, dto));
    }

    @Operation(summary = "Activar o desactivar un valor del sistema")
    @PatchMapping("/valores/{uuidValor}/estatus")
    public ResponseEntity<ApiDataResponseDTO<AtributoValorResponseDTO>> cambiarEstatusValor(@PathVariable UUID uuidValor) {
        return okActualizado(atributoService.cambiarEstatusValorGlobal(uuidValor));
    }

    @Operation(summary = "Eliminar un valor del sistema", description = "Si alguna variante lo usa, se desactiva.")
    @DeleteMapping("/valores/{uuidValor}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminarValor(@PathVariable UUID uuidValor) {
        atributoService.eliminarValorGlobal(uuidValor);
        return okEliminado();
    }
}

