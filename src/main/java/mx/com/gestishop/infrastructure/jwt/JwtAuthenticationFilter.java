package mx.com.gestishop.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Intercepta cada request, valida el token
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token != null && jwtTokenProvider.esTokenValido(token)) {
            Claims claims = jwtTokenProvider.validarYExtraerClaims(token);

            String tipoUsuario = claims.get("tipoUsuario", String.class);
            // El "rol" de Spring Security se prefija con ROLE_ por convención
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + tipoUsuario.toUpperCase()));

            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),   // principal = uuid del usuario
                    null,
                    authorities
            );
            authentication.setDetails(claims);  // Guardar todos los claims por si se necesitan después

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}