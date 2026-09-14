package mx.com.gestishop.infrastructure.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.domain.model.Usuario;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

// Genera y valida el access token
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
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
