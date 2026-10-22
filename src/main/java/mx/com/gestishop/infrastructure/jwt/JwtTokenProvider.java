package mx.com.gestishop.infrastructure.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.domain.model.Usuario;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

// Genera y valida el access token
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Falla al ARRANCAR la aplicación si el secret no sirve (vacío, sin resolver o muy corto),
     * en lugar de fallar en el primer login. Caso típico: un perfil sin 'cf.app.jwt.secret'
     * deja el texto literal '@cf.app.jwt.secret@' (19 caracteres) como secret.
     * HS256 exige al menos 32 bytes (256 bits).
     */
    @PostConstruct
    void validarSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.startsWith("@") || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret no está configurado o tiene menos de 32 caracteres. Revisa el perfil activo.");
        }
    }

    // Llave con la que se firman y validan los tokens (siempre UTF-8, sin depender del SO del servidor)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generarAccessToken(Usuario usuario, Long idNegocio, String nivelAcceso, List<String> modulos) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtProperties.getAccessTokenExpirationMs());

        JwtBuilder builder = Jwts.builder()
                .subject(usuario.getUuidUsuario().toString())
                .claim("tipoUsuario", usuario.getTipoUsuario())
                .claim("nombre", usuario.getNombre())
                .claim("correo", usuario.getCorreo())
                .claim("modulos", modulos)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(getSigningKey());

        if (idNegocio != null) builder.claim("idNegocio", idNegocio);
        if (nivelAcceso != null) builder.claim("nivelAcceso", nivelAcceso);

        return builder.compact();
    }

    public Claims validarYExtraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esTokenValido(String token) {
        try {
            validarYExtraerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extraerUuidUsuario(String token) {
        return validarYExtraerClaims(token).getSubject();
    }
}
