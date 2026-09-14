package mx.com.gestishop.infrastructure.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Lee secret/expiración desde application.properties
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;                  // secret key, minimum 256 bits, comes from application.properties
    private long accessTokenExpirationMs;    // ej. 900000 (15 min)
    private long refreshTokenExpirationMs;   // ej. 604800000 (7 días)
}
