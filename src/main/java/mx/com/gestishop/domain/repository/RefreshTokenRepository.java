package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

// Acceso a datos de la tabla refresh_tokens (sesiones activas revocables)
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca una sesión de refresh token por su hash (nunca se busca por el token en claro).
     * Se usa al refrescar el access token: valida que exista, no esté revocado y no haya expirado.
     */
    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.tokenHash = :tokenHash
          AND rt.revocado = false
          AND rt.fechaExpiracion > :ahora
        """)
    Optional<RefreshToken> buscarValidoPorHash(@Param("tokenHash") String tokenHash,
                                               @Param("ahora") OffsetDateTime ahora);

    /**
     * Revoca (invalida) una sesión específica por su hash. Se usa en logout normal
     * y también al rotar un token (el viejo se revoca al emitir uno nuevo).
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revocado = true, rt.fechaRevocado = :ahora
        WHERE rt.tokenHash = :tokenHash
        """)
    void revocarPorHash(@Param("tokenHash") String tokenHash, @Param("ahora") OffsetDateTime ahora);

    /**
     * Revoca TODAS las sesiones activas de un usuario. Se dispara al cambiar
     * contraseña, o si se detecta uso de un refresh token ya rotado (posible robo).
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revocado = true, rt.fechaRevocado = :ahora
        WHERE rt.usuario.idUsuario = :idUsuario
          AND rt.revocado = false
        """)
    void revocarTodasDelUsuario(@Param("idUsuario") Long idUsuario, @Param("ahora") OffsetDateTime ahora)
}
