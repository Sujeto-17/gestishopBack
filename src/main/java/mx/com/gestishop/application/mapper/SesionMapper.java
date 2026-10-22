package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.ContextoSesionDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;
import mx.com.gestishop.application.dto.response.SesionResponseDTO;
import mx.com.gestishop.domain.model.Usuario;

import java.util.List;

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

    /**
     * Arma la respuesta de login/renovación/cambio de negocio. Vive aquí, en un solo lugar, para que
     * los tres flujos respondan exactamente lo mismo (antes se repetía el builder y un campo se
     * olvidó en la renovación).
     */
    public static LoginResponseDTO toLoginResponse(Usuario usuario, ContextoSesionDTO contexto,
                                                   String accessToken, String refreshToken) {
        return LoginResponseDTO.builder()
                .uuidUsuario(usuario.getUuidUsuario())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .debeActualizarPassword(usuario.getDebeActualizarPassword())
                .tipoUsuario(usuario.getTipoUsuario())
                .nivelAcceso(contexto.getNivelAcceso())
                .idNegocio(contexto.getIdNegocio())
                .modulos(contexto.getModulos())
                .negocios(contexto.getNegocios() != null ? contexto.getNegocios() : List.of())
                .build();
    }
}