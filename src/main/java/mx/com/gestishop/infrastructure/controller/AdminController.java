package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.AdminResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestión de administradores ya registrados: consulta, activación y
 * restablecimiento de contraseña. El alta de un admin nuevo se hace desde
 * UsuarioController (POST /usuarios), ya que ese endpoint es genérico
 * para los 3 tipos de usuario.
 */
@Tag(name = "Administradores", description = "Consulta y gestión de cuentas de administrador")
@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminController extends BaseController {

    private final AdminService adminService;

    @Operation(summary = "Listar todos los administradores", description = "Incluye el negocio asignado a cada uno.")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<AdminResponseDTO>>> listarTodos() {
        return ok(adminService.listarTodos());
    }

    @Operation(summary = "Obtener un administrador por su identificador")
    @GetMapping("/{uuidUsuario}")
    public ResponseEntity<ApiDataResponseDTO<AdminResponseDTO>> obtener(@PathVariable UUID uuidUsuario) {
        return okEncontrado(adminService.obtenerPorUuid(uuidUsuario));
    }

    @Operation(summary = "Activar o desactivar un administrador")
    @PatchMapping("/{uuidUsuario}/estatus")
    public ResponseEntity<ApiDataResponseDTO<AdminResponseDTO>> cambiarEstatus(@PathVariable UUID uuidUsuario) {
        return okActualizado(adminService.cambiarEstatus(uuidUsuario));
    }

    @Operation(summary = "Restablecer la contraseña de un administrador",
            description = "Genera una contraseña temporal aleatoria y la devuelve en texto plano UNA sola vez. " +
                    "El admin deberá cambiarla en su próximo login. Comunícala por un canal seguro.")
    @PostMapping("/{uuidUsuario}/restablecer-password")
    public ResponseEntity<ApiDataResponseDTO<Map<String, String>>> restablecerPassword(
            @PathVariable UUID uuidUsuario) {
        String passwordTemporal = adminService.restablecerPassword(uuidUsuario);
        return ok(Map.of("passwordTemporal", passwordTemporal));
    }
}