package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.TrabajadorRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.TrabajadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de trabajadores. Solo accesible por admin (el dueño del negocio da
 * de alta a su propio personal). El nivel de acceso permitido es
 * "gerente" o "estandar" — nunca "admin" ni "superadmin", que se dan de
 * alta desde UsuarioController.
 */
@Tag(name = "Trabajadores", description = "Alta y gestión del personal de un negocio")
@RestController
@RequestMapping("/api/v1/trabajadores")
@RequiredArgsConstructor
public class TrabajadorController extends BaseController {

    private final TrabajadorService trabajadorService;

    @Operation(summary = "Registrar un trabajador nuevo",
            description = "Solo admin. Crea la cuenta de acceso y el registro laboral en una sola operación.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> crear(
            @Valid @RequestBody TrabajadorRequestDTO dto) {
        return okCreado(trabajadorService.crear(dto));
    }

    @Operation(summary = "Actualizar datos de un trabajador", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{uuidUsuario}")
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> actualizar(
            @PathVariable UUID uuidUsuario, @Valid @RequestBody TrabajadorRequestDTO dto) {
        return okActualizado(trabajadorService.actualizar(uuidUsuario, dto));
    }

    @Operation(summary = "Activar o desactivar el acceso de un trabajador", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{uuidUsuario}/estatus")
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> cambiarEstatus(
            @PathVariable UUID uuidUsuario) {
        return okActualizado(trabajadorService.cambiarEstatus(uuidUsuario));
    }

    @Operation(summary = "Obtener un trabajador por su identificador", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{uuidUsuario}")
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> obtener(
            @PathVariable UUID uuidUsuario) {
        return okEncontrado(trabajadorService.obtenerPorUuid(uuidUsuario));
    }

    @Operation(summary = "Listar trabajadores de un negocio", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<TrabajadorResponseDTO>>> listarPorNegocio(
            @RequestParam Long idNegocio) {
        return ok(trabajadorService.listarPorNegocio(idNegocio));
    }
}
