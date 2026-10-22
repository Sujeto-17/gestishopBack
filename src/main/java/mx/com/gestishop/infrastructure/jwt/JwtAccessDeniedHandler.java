package mx.com.gestishop.infrastructure.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.generic.ApiResponseBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Respuesta cuando el usuario SÍ está autenticado pero NO tiene permiso para esa URL
 * (por ejemplo, un admin intentando entrar a /api/v1/negocios).
 * <p>
 * Es la pareja de JwtAuthenticationEntryPoint (que cubre el 401): sin esta clase Spring
 * devolvía un 403 vacío, y el front mostraba "No se pudo conectar con el servidor".
 * Aquí se responde con el mismo formato JSON estándar de toda la API.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        var apiResponse = ApiResponseBuilder.build(ApiCodeResponse.FORBIDDEN, null);

        response.setStatus(apiResponse.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), apiResponse.getBody());
    }

}
