package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CambiarPasswordRequestDTO;
import mx.com.gestishop.application.dto.request.LoginRequestDTO;
import mx.com.gestishop.application.dto.request.RefreshTokenRequestDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Controlador de autenticación
@Tag(name = "Autenticación", description = "Login, renovación de sesión, logout y cambio de contraseña")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

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
        authService.cambiarPassword(dto);
        return ok();
    }
}
