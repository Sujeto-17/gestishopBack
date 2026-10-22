package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CambiarNegocioRequestDTO;
import mx.com.gestishop.application.dto.request.CambiarPasswordRequestDTO;
import mx.com.gestishop.application.dto.request.LoginRequestDTO;
import mx.com.gestishop.application.dto.request.RefreshTokenRequestDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.AuthService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controlador de autenticación
@Tag(name = "Autenticación", description = "Login, renovación de sesión, logout y cambio de contraseña")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;
    private final SesionActual sesionActual;  // quién es el usuario autenticado (viene del token)

    @Operation(summary = "Iniciar sesión",
            description = "Valida credenciales y devuelve un par de tokens (access + refresh). " +
                    "Aplica para superadmin, admin y trabajador; el tipo se resuelve internamente.")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "409", description = "Credenciales inválidas, cuenta inactiva o bloqueo por intentos fallidos")
    @PostMapping("/login")
    public ResponseEntity<ApiDataResponseDTO<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request) {

        // Se capturan IP y User-Agent para auditoría del refresh token (tabla refresh_tokens)
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        return ok(authService.login(dto, ip, userAgent));
    }

    @Operation(summary = "Renovar sesión",
            description = "Intercambia un refresh token válido por un nuevo par access/refresh. " +
                    "El refresh token usado queda revocado (rotación).")
    @PostMapping("/refresh")
    public ResponseEntity<ApiDataResponseDTO<LoginResponseDTO>> refrescar(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ok(authService.refrescarToken(dto.getRefreshToken()));
    }

    @Operation(summary = "Cerrar sesión",
            description = "Revoca el refresh token indicado. El access token sigue vivo hasta que expire " +
                    "(máximo 15 minutos), por eso su vida es intencionalmente corta.")
    @PostMapping("/logout")
    public ResponseEntity<ApiDataResponseDTO<Void>> logout(
            @Valid @RequestBody RefreshTokenRequestDTO dto) {
        authService.logout(dto.getRefreshToken());
        return ok();
    }

    @Operation(summary = "Cambiar contraseña",
            description = "Cambia la contraseña del usuario autenticado y revoca todas sus sesiones activas por seguridad.")
    @PostMapping("/cambiar-password")
    public ResponseEntity<ApiDataResponseDTO<Void>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequestDTO dto) {
        // Solo se puede cambiar la contraseña de la PROPIA cuenta: el uuid del body debe coincidir
        // con el del token. Antes el uuid del body mandaba, y el endpoint era público.
        if (!sesionActual.uuidUsuario().equals(dto.getUuidUsuario())) {
            throw new ApiResponseException(ApiCodeResponse.FORBIDDEN);
        }
        authService.cambiarPassword(dto);
        return ok();
    }

    @Operation(summary = "Cambiar de negocio",
            description = "Solo administradores con más de un negocio (dueño con sucursales). Valida que el negocio sea suyo " +
                    "y esté vigente, y devuelve un par de tokens nuevo con los módulos de ese negocio. " +
                    "El refresh token enviado se revoca (rotación).")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/negocio-activo")
    public ResponseEntity<ApiDataResponseDTO<LoginResponseDTO>> cambiarNegocio(
            @Valid @RequestBody CambiarNegocioRequestDTO dto) {
        return ok(authService.cambiarNegocio(sesionActual.uuidUsuario(), dto));
    }
}
