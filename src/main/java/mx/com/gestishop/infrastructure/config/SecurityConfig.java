package mx.com.gestishop.infrastructure.config;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.infrastructure.jwt.JwtAccessDeniedHandler;
import mx.com.gestishop.infrastructure.jwt.JwtAuthenticationEntryPoint;
import mx.com.gestishop.infrastructure.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Configuración central de seguridad: JWT sin sesión (stateless), CORS y reglas por URL.
 * <p>
 * Hay DOS capas de autorización y se necesitan las dos:
 *  1) Reglas por URL (aquí abajo): la primera barrera, por tipo de usuario.
 *  2) @PreAuthorize en cada controller: reglas finas (módulo, nivel de acceso).
 *     SOLO funcionan con @EnableMethodSecurity (ver más abajo). Antes de esta corrección
 *     esa anotación no estaba en el proyecto, así que TODOS los @PreAuthorize se ignoraban
 *     y cualquier usuario con sesión podía llamar endpoints de superadmin.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // <-- activa @PreAuthorize / @PostAuthorize en los controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;   // 401 en JSON
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;             // 403 en JSON

    // Orígenes permitidos por CORS, separados por coma. En producción se define con la propiedad
    // app.cors.allowed-origins (ej. https://app.gestishop.mx); si no existe, solo el front local.
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private List<String> origenesPermitidos;

    // Encriptador de contraseñas (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt con factor de trabajo 12 (balance seguridad/rendimiento)
        return new BCryptPasswordEncoder(12);
    }

    // Cadena de filtros: qué URL exige qué y cómo se responde cuando no hay permiso
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // API stateless con JWT no necesita CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // sin token / token inválido -> 401
                        .accessDeniedHandler(jwtAccessDeniedHandler))            // con token pero sin permiso -> 403
                .authorizeHttpRequests(auth -> auth
                        // --- Públicos ---
                        // Solo login, refresh y logout. /auth/cambiar-password YA NO es público:
                        // exige sesión y solo permite cambiar la contraseña de la propia cuenta.
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/archivos/ver/**").permitAll()
                        // Sin esto, cualquier error que Spring reenvía a /error se vuelve 401/403
                        .requestMatchers("/error").permitAll()

                        // --- Solo superadmin ---
                        .requestMatchers("/api/v1/negocios/**", "/api/v1/planes/**", "/api/v1/admins/**",
                                "/api/v1/usuarios/**", "/api/v1/suscripciones/**").hasRole("SUPERADMIN")

                        // --- Admin y trabajadores (el filtro fino por módulo/nivel va en @PreAuthorize) ---
                        .requestMatchers("/api/v1/productos/**", "/api/v1/proveedores/**", "/api/v1/categorias/**")
                        .hasAnyRole("ADMIN", "TRABAJADOR")

                        // Todo lo demás exige estar autenticado (y cada controller afina con @PreAuthorize)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS: desde qué dominios puede llamar el front a esta API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenesPermitidos);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
