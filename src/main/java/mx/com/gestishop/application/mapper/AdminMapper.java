package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.AdminResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.AdminNegocio;

// Convierte una relación AdminNegocio (usuario + negocio asociado) al DTO plano que consume el frontend
public class AdminMapper {

    private AdminMapper() {
    }

    public static AdminResponseDTO toResponse(AdminNegocio relacion) {
        var usuario = relacion.getUsuario();
        var negocio = relacion.getNegocio();

        return AdminResponseDTO.builder()
                .uuidUsuario(usuario.getUuidUsuario())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .negocio(CatalogoRefDTO.builder()
                        .id(negocio.getIdNegocio().intValue())
                        .nombre(negocio.getNombre())
                        .build())
                .estatus(usuario.getEstatus())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .fechaAlta(usuario.getFechaAlta())
                .build();
    }
}