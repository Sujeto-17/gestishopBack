package mx.com.gestishop.infrastructure.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API GESTISHOP")
                        .version("1.0.0")
                        .description("API - Sistema de Control de Tiendas y Comercio en general")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email(""))
                        .license(new License()
                                .name("Uso Público para las tiendas y comercios")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación Técnica")
                        .url(""));
    }
}
