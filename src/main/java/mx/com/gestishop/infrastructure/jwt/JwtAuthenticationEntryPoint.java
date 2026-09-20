package mx.com.gestishop.infrastructure.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.generic.ApiResponseBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

/**
 * Punto de entrada de autenticación de Spring Security.
 * <p>
 * Se activa automáticamente cuando una petición llega a un endpoint protegido
 * SIN un JWT válido (ausente, expirado o malformado). Sin esta clase, Spring
 * Security devolvería un error 403 genérico en HTML/texto plano; aquí lo
 * traducimos al mismo formato JSON estándar que usa el resto de la API
 * (ApiDataResponseDTO), para que el frontend siempre reciba la misma forma
 * de respuesta sin importar el tipo de error.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // ObjectMapper para serializar manualmente la respuesta a JSON,
    // ya que en este punto estamos fuera del ciclo normal de Spring MVC
    // (el filtro de seguridad se ejecuta antes de que un @RestController pueda responder).
    private final ObjectMapper objectMapper;

    /**
     * Se ejecuta cada vez que una petición no autenticada intenta acceder
     * a un recurso protegido.
     *
     * @param request       la petición HTTP original
     * @param response      la respuesta HTTP que vamos a construir manualmente
     * @param authException la excepción de Spring Security que disparó este flujo
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // Armamos la respuesta usando el mismo builder que usa el resto de la API,
        // así el error de "no autenticado" tiene la misma forma que cualquier otro error.
        var apiResponse = ApiResponseBuilder.build(ApiCodeResponse.UNAUTHORIZED, null);

        response.setStatus(apiResponse.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Escribimos el cuerpo del ApiDataResponseDTO directamente en el stream de respuesta
        objectMapper.writeValue(response.getWriter(), apiResponse.getBody());
    }
}
