package mx.com.gestishop.infrastructure.controller;

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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiDataResponseDTO<LoginResponseDTO>> login(
            @RequestBody @Valid LoginRequestDTO dto,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            jakarta.servlet.http.HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        return ok(authService.login(dto, ip, userAgent));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiDataResponseDTO<LoginResponseDTO>> refrescar(
            @RequestBody @Valid RefreshTokenRequestDTO dto) {
        return ok(authService.refrescarToken(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiDataResponseDTO<Void>> logout(
            @RequestBody @Valid RefreshTokenRequestDTO dto) {
        authService.logout(dto.getRefreshToken());
        return ok();
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<ApiDataResponseDTO<Void>> cambiarPassword(
            @RequestBody @Valid CambiarPasswordRequestDTO dto) {
        authService.cambiarPassword(dto);
        return ok();
    }
}
