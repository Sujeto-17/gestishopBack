package mx.com.gestishop.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Contenedor interno con el contexto resuelto de un usuario autenticado.
 * NO es un DTO de la API pública (sin anotaciones Swagger) — es un valor
 * intermedio compartido entre AuthServiceImpl y RefreshTokenServiceImpl,
 * ambos necesitan lo mismo (negocio, nivel de acceso, módulos) y así se
 * evita duplicar el switch por tipo de usuario en dos archivos distintos.
 */
@Getter
@Builder
public class ContextoSesionDTO {

    //ID del negocio al que pertenece (null si es superadmin)
    private final Long idNegocio;

    // Nivel de acceso (gerente | estandar), solo aplica si es trabajador
    private final String nivelAcceso;

    // Nombres de los módulos que este usuario puede ver en el menú
    private final List<String> modulos;
}
