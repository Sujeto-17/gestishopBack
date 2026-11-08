package mx.com.gestishop.application.dto;

import lombok.Builder;
import lombok.Getter;
import mx.com.gestishop.application.dto.response.NegocioSesionDTO;

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

    // Nombre del negocio activo, para mostrarlo en el pie de página (null si es superadmin)
    private final String nombreNegocio;

    // Nivel de acceso (gerente | estandar), solo aplica si es trabajador
    private final String nivelAcceso;

    // CLAVES de los módulos que este usuario puede ver (ej. "productos"), no sus nombres: el nombre
    // se puede editar en pantalla, la clave nunca.
    private final List<String> modulos;

    // Negocios a los que puede entrar un ADMIN (para el selector). Vacío para los demás tipos.
    private final List<NegocioSesionDTO> negocios;
}
