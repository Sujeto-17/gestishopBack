package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.response.LoginResponseDTO;
import mx.com.gestishop.domain.model.Usuario;

/**
 * Contrato para el manejo del ciclo de vida de las sesiones (refresh tokens):
 * creación, rotación (renovar el access token) y revocación (logout, cambio
 * de contraseña, o detección de robo de sesión).
 */
public interface RefreshTokenService {

    /**
     * Genera una nueva sesión de refresh token para un usuario recién autenticado.
     * @return el token en texto plano (única vez que se ve sin hashear; el cliente lo guarda).
     */
    String crear(Usuario usuario, String ip, String userAgent);

    /**
     * Intercambia un refresh token válido por un nuevo par access/refresh (rotación).
     * Si detecta reutilización de un token ya revocado, revoca TODAS las sesiones
     * del usuario por seguridad (posible robo de sesión).
     */
    LoginResponseDTO rotar(String refreshTokenPlano);

    /** Revoca una sesión específica (logout normal). */
    void revocar(String refreshTokenPlano);

    /** Revoca todas las sesiones activas de un usuario (cambio de password, robo detectado). */
    void revocarTodasDelUsuario(Long idUsuario);
}
