package mx.com.gestishop.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger. Además de los metadatos generales,
 * define el esquema de seguridad "bearerAuth" para que la interfaz de
 * Swagger muestre el botón "Authorize" y permita probar endpoints
 * protegidos pegando el access token obtenido en /auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    // http://localhost:8080/swagger-ui/index.html

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API GESTISHOP")
                        .version("1.0.0")
                        .description("API - Sistema de Control de Tiendas y Comercio en general")
                        .contact(new Contact().name("Equipo de Desarrollo").email(""))
                        .license(new License().name("Uso Público para las tiendas y comercios")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación Técnica")
                        .url(""))
        // Define el esquema JWT: tipo Bearer, formato JWT
                .components(new Components()
                .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .name(ESQUEMA_JWT)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // Aplica el esquema por defecto a TODOS los endpoints documentados
                // (los que son públicos, como /auth/login, igual muestran el candado,
                // pero no requieren token para funcionar realmente)
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
