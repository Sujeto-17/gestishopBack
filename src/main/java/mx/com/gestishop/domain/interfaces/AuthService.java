package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.CambiarPasswordRequestDTO;
import mx.com.gestishop.application.dto.request.LoginRequestDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;

public interface AuthService {

    /**
     * Valida credenciales y, si son correctas, resuelve el contexto del usuario
     * (negocio, nivel de acceso, módulos permitidos) y emite un par de tokens.
     *
     * @param dto       correo y contraseña enviados por el cliente
     * @param ip        IP de origen de la petición, se guarda para auditoría del refresh token
     * @param userAgent User-Agent del navegador/app que hace login, mismo propósito de auditoría
     */
    LoginResponseDTO login(LoginRequestDTO dto, String ip, String userAgent);

    /**
     * Renueva la sesión intercambiando un refresh token válido por un nuevo
     * par access/refresh. La lógica de rotación vive en RefreshTokenService.
     */
    LoginResponseDTO refrescarToken(String refreshTokenPlano);

    /**
     * Cierra sesión revocando el refresh token indicado.
     * El access token sigue siendo válido hasta que expire por sí solo
     * (máximo 15 min), por eso su vida útil es intencionalmente corta.
     */
    void logout(String refreshTokenPlano);

    /**
     * Cambia la contraseña del usuario autenticado.
     * Requiere validar la contraseña actual antes de permitir el cambio.
     * Al finalizar, revoca TODAS las sesiones activas del usuario como
     * medida de seguridad (por si el cambio se debe a una sospecha de robo).
     */
    void cambiarPassword(CambiarPasswordRequestDTO dto);
}
