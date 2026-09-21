package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.TrabajadorRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorResponseDTO;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Trabajador;
import mx.com.gestishop.domain.model.Usuario;

/**
 * Convierte entre las entidades Usuario/Trabajador (dos tablas relacionadas
 * 1 a 1) y el DTO combinado que expone la API como una sola operación.
 */
public class TrabajadorMapper {

    private TrabajadorMapper() {
    }

    // Construye la parte de identidad (tabla usuarios) a partir del DTO combinado
    public static Usuario toUsuarioEntity(TrabajadorRequestDTO dto, String passwordHash) {
        return Usuario.builder()
                .tipoUsuario("trabajador")
                .nombre(dto.getNombre())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .passwordHash(passwordHash)
                .debeActualizarPassword(true)  // toda cuenta nueva fuerza cambio de contraseña
                .estatus("activo")
                .build();
    }

    // Construye la parte laboral (tabla trabajadores), ligada al Usuario y Negocio ya guardados
    public static Trabajador toTrabajadorEntity(TrabajadorRequestDTO dto, Usuario usuario, Negocio negocio) {
        return Trabajador.builder()
                .usuario(usuario)
                .negocio(negocio)
                .curp(dto.getCurp())
                .nss(dto.getNss())
                .puesto(dto.getPuesto())
                .rol(dto.getRol())
                .nivelAcceso(dto.getNivelAcceso())
                .turno(dto.getTurno())
                .tipoContrato(dto.getTipoContrato())
                .salario(dto.getSalario())
                .notas(dto.getNotas())
                .build();
    }

    // Combina ambas entidades (Trabajador trae su Usuario asociado vía relación 1:1) en un solo DTO de salida
    public static TrabajadorResponseDTO toResponse(Trabajador trabajador) {
        Usuario usuario = trabajador.getUsuario();

        return TrabajadorResponseDTO.builder()
                .idTrabajador(trabajador.getIdTrabajador())
                .uuidUsuario(usuario.getUuidUsuario())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .puesto(trabajador.getPuesto())
                .rol(trabajador.getRol())
                .nivelAcceso(trabajador.getNivelAcceso())
                .turno(trabajador.getTurno())
                .tipoContrato(trabajador.getTipoContrato())
                .salario(trabajador.getSalario())
                .curp(trabajador.getCurp())
                .nss(trabajador.getNss())
                .notas(trabajador.getNotas())
                .estatus(usuario.getEstatus())
                .fechaIngreso(trabajador.getFechaIngreso())
                .idNegocio(trabajador.getNegocio().getIdNegocio())
                .build();
    }
}
