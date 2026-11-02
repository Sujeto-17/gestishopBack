package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.TrabajadorBaseDTO;
import mx.com.gestishop.application.dto.request.TrabajadorActualizarRequestDTO;
import mx.com.gestishop.application.dto.request.TrabajadorRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.*;

/**
 * Convierte entre las entidades Usuario/Trabajador (dos tablas relacionadas
 * 1 a 1) y el DTO combinado que expone la API como una sola operación.
 */
public class TrabajadorMapper {

    private TrabajadorMapper() {
    }

    // Identidad de acceso (tabla usuarios)
    public static Usuario toUsuarioEntity(TrabajadorRequestDTO dto, String passwordHash) {
        return Usuario.builder()
                .tipoUsuario("trabajador")
                .nombre(dto.getNombre())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .passwordHash(passwordHash)
                .debeActualizarPassword(true)
                .estatus("activo")
                .build();
    }

    // Registro laboral (tabla trabajadores), ligado al usuario y negocio ya guardados
    public static Trabajador toTrabajadorEntity(TrabajadorRequestDTO dto, Usuario usuario, Negocio negocio) {
        Trabajador trabajador = Trabajador.builder()
                .usuario(usuario)
                .negocio(negocio)
                .build();
        aplicarDatos(trabajador, dto);
        return trabajador;
    }

    // Edición: nombre/teléfono viven en usuarios; el resto en trabajadores. Correo y password NO se tocan
    public static void actualizarEntidad(Trabajador entidad, Usuario usuario, TrabajadorActualizarRequestDTO dto) {
        usuario.setNombre(dto.getNombre());
        usuario.setTelefono(dto.getTelefono());
        aplicarDatos(entidad, dto);
    }

    // Único lugar donde se copian los datos del DTO a la entidad
    private static void aplicarDatos(Trabajador t, TrabajadorBaseDTO dto) {
        boolean formal = "formal".equals(dto.getRegimen());
        boolean transferencia = "transferencia".equals(dto.getMetodoPago());

        // Personales
        t.setFechaNacimiento(dto.getFechaNacimiento());
        t.setDireccion(dto.getDireccion());
        t.setCurp(dto.getCurp());
        t.setNss(dto.getNss());

        // Contacto de emergencia
        t.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        t.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        t.setContactoEmergenciaParentesco(dto.getContactoEmergenciaParentesco());

        // Laborales
        t.setPuesto(dto.getPuesto());
        t.setNivelAcceso(dto.getNivelAcceso());
        t.setTipoContrato(dto.getTipoContrato());
        t.setNotas(dto.getNotas());

        // Pago: si deja de pagarse por transferencia, se borran los datos bancarios
        t.setSalario(dto.getSalario());
        t.setPeriodicidadPago(dto.getPeriodicidadPago());
        t.setMetodoPago(dto.getMetodoPago());
        t.setBanco(transferencia ? dto.getBanco() : null);
        t.setClabe(transferencia ? dto.getClabe() : null);

        // Régimen: si pasa a informal, se limpian los datos exclusivos de nómina formal
        t.setRegimen(dto.getRegimen());
        t.setRfc(formal ? dto.getRfc() : null);
        t.setNumeroEmpleado(formal ? dto.getNumeroEmpleado() : null);
        t.setSalarioDiarioIntegrado(formal ? dto.getSalarioDiarioIntegrado() : null);
        t.setFechaAltaImss(formal ? dto.getFechaAltaImss() : null);
        t.setDiasAguinaldo(formal ? dto.getDiasAguinaldo() : null);
        t.setPrimaVacacionalPct(formal ? dto.getPrimaVacacionalPct() : null);
    }

    public static TrabajadorResponseDTO toResponse(Trabajador t) {
        Usuario usuario = t.getUsuario();

        return TrabajadorResponseDTO.builder()
                .idTrabajador(t.getIdTrabajador())
                .uuidUsuario(usuario.getUuidUsuario())
                .idNegocio(t.getNegocio().getIdNegocio())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .estatus(usuario.getEstatus())
                .fechaNacimiento(t.getFechaNacimiento())
                .direccion(t.getDireccion())
                .genero(mapearGenero(t.getGenero()))
                .estadoCivil(mapearEstadoCivil(t.getEstadoCivil()))
                .municipio(mapearMunicipio(t.getMunicipio()))
                .curp(t.getCurp())
                .nss(t.getNss())
                .contactoEmergenciaNombre(t.getContactoEmergenciaNombre())
                .contactoEmergenciaTelefono(t.getContactoEmergenciaTelefono())
                .contactoEmergenciaParentesco(t.getContactoEmergenciaParentesco())
                .puesto(t.getPuesto())
                .nivelAcceso(t.getNivelAcceso())
                .tipoContrato(t.getTipoContrato())
                .fechaIngreso(t.getFechaIngreso())
                .notas(t.getNotas())
                .salario(t.getSalario())
                .periodicidadPago(t.getPeriodicidadPago())
                .metodoPago(t.getMetodoPago())
                .banco(t.getBanco())
                .clabe(t.getClabe())
                .regimen(t.getRegimen())
                .rfc(t.getRfc())
                .numeroEmpleado(t.getNumeroEmpleado())
                .salarioDiarioIntegrado(t.getSalarioDiarioIntegrado())
                .fechaAltaImss(t.getFechaAltaImss())
                .diasAguinaldo(t.getDiasAguinaldo())
                .primaVacacionalPct(t.getPrimaVacacionalPct())
                .build();
    }

    // Métodos privados
    private static CatalogoRefDTO mapearGenero(CatGenero g) {
        return g == null ? null
                : CatalogoRefDTO.builder().id(g.getIdGenero().intValue()).nombre(g.getNombre()).build();
    }

    private static CatalogoRefDTO mapearEstadoCivil(CatEstadoCivil e) {
        return e == null ? null
                : CatalogoRefDTO.builder().id(e.getIdEstadoCivil().intValue()).nombre(e.getNombre()).build();
    }

    private static CatalogoRefDTO mapearMunicipio(CatMunicipio m) {
        return m == null ? null
                : CatalogoRefDTO.builder().id(m.getIdMunicipio().intValue()).nombre(m.getNombre()).build();
    }
}