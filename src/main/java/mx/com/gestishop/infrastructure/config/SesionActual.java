package mx.com.gestishop.infrastructure.config;

import io.jsonwebtoken.Claims;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lee la sesión del usuario autenticado desde los claims del JWT que
 * JwtAuthenticationFilter dejó en el SecurityContext.
 * Uso en seguridad: @PreAuthorize("@sesionActual.tieneModulo('Proveedores')")
 */
@Component("sesionActual")
public class SesionActual {

    private Claims claimsOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getDetails() instanceof Claims c ? c : null;
    }

    private Claims claims() {
        Claims c = claimsOrNull();
        if (c == null) throw new ApiResponseException(ApiCodeResponse.UNAUTHORIZED);
        return c;
    }

    public UUID uuidUsuario() {
        return UUID.fromString(claims().getSubject());
    }

    public String tipoUsuario() {
        Claims c = claimsOrNull();
        return c == null ? null : c.get("tipoUsuario", String.class);
    }

    public String nivelAcceso() {
        Claims c = claimsOrNull();
        return c == null ? null : c.get("nivelAcceso", String.class);
    }

    public boolean esGerente() {
        return "gerente".equals(nivelAcceso());
    }

    public Long idNegocio() {
        return claims().get("idNegocio") instanceof Number n ? n.longValue() : null;
    }

    /** Para endpoints que solo tienen sentido con un negocio (admin y trabajadores). */
    public Long idNegocioObligatorio() {
        Long id = idNegocio();
        if (id == null) {
            throw new ApiResponseException(ApiCodeResponse.FORBIDDEN,
                    Map.of("detalle", "Tu cuenta no está asociada a ningún negocio."));
        }
        return id;
    }

    public List<String> modulos() {
        Claims c = claimsOrNull();
        return c != null && c.get("modulos") instanceof List<?> lista
                ? lista.stream().map(String::valueOf).toList()
                : List.of();
    }

    // ¿Tiene el módulo con esa CLAVE? (ej. tieneModulo("productos")). Se compara por clave, nunca por nombre.
    public boolean tieneModulo(String clave) {
        return modulos().contains(clave);
    }

    public boolean esAdmin() {
        return "admin".equals(tipoUsuario());
    }
}