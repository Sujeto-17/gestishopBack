package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.SesionResponseDTO;
import mx.com.gestishop.domain.model.Usuario;

/**
 * Convierte entre la entidad Usuario y sus DTOs de respuesta relacionados
 * con identidad/sesión. Clase de métodos estáticos (sin estado ni
 * dependencias) porque el mapeo es directo y no requiere consultar otras tablas.
 */
public class SesionMapper {

    private SesionMapper() {
    }

    /**
     * Construye el resumen de identidad de un usuario recién registrado.
     * Nunca incluye el passwordHash ni ningún dato sensible.
     */
    public static SesionResponseDTO toResponse(Usuario usuario) {
        return SesionResponseDTO.builder()
                .uuidUsuario(usuario.getUuidUsuario())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .tipoUsuario(usuario.getTipoUsuario())
                .build();
    }
}
