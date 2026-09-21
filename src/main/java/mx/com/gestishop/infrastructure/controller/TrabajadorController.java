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
    @PutMapping("/{idTrabajador}")
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> actualizar(
            @PathVariable Long idTrabajador, @Valid @RequestBody TrabajadorRequestDTO dto) {
        return okActualizado(trabajadorService.actualizar(idTrabajador, dto));
    }

    @Operation(summary = "Activar o desactivar el acceso de un trabajador", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{idTrabajador}/estatus")
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> cambiarEstatus(
            @PathVariable Long idTrabajador) {
        return okActualizado(trabajadorService.cambiarEstatus(idTrabajador));
    }

    @Operation(summary = "Obtener un trabajador por su identificador", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{idTrabajador}")
    public ResponseEntity<ApiDataResponseDTO<TrabajadorResponseDTO>> obtener(
            @PathVariable Long idTrabajador) {
        return okEncontrado(trabajadorService.obtenerPorId(idTrabajador));
    }

    @Operation(summary = "Listar trabajadores de un negocio", description = "Solo admin.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<TrabajadorResponseDTO>>> listarPorNegocio(
            @RequestParam Long idNegocio) {
        return ok(trabajadorService.listarPorNegocio(idNegocio));
    }
}
