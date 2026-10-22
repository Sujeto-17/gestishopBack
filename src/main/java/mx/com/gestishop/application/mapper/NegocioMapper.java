package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.NegocioRequestDTO;
import mx.com.gestishop.application.dto.response.NegocioResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.CatGiroNegocio;
import mx.com.gestishop.domain.model.CatMunicipio;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.infrastructure.utils.FechaNegocioUtil;

import java.time.LocalDate;

/**
 * Convierte entre la entidad Negocio y sus DTOs. La fecha de vencimiento
 * NUNCA se captura manualmente en el DTO de entrada — todo negocio nuevo
 * entra en periodo de prueba de 15 días, calculado aquí, para evitar que
 * alguien capture una fecha arbitraria que rompa la lógica de vencimientos.
 */
public class NegocioMapper {

    private static final int DIAS_PRUEBA = 15;

    private NegocioMapper() {
    }

    // Construye la entidad; giro y plan se asignan aparte en el service (requieren consulta a BD)
    public static Negocio toEntity(NegocioRequestDTO dto) {
        return Negocio.builder()
                .nombre(dto.getNombre())
                .logoUrl(dto.getLogoUrl())
                .sistemaType(dto.getSistemaType())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .direccion(dto.getDireccion())
                .razonSocial(dto.getRazonSocial())
                .rfc(dto.getRfc())
                .regimenFiscal(dto.getRegimenFiscal())
                .codigoPostalFiscal(dto.getCodigoPostalFiscal())
                .correoFacturacion(dto.getCorreoFacturacion())
                .notasInternas(dto.getNotasInternas())
                .estatus("prueba")  // todo negocio nuevo inicia en periodo de prueba
                .fechaAlta(FechaNegocioUtil.hoy())
                .vencimiento(FechaNegocioUtil.hoy().plusDays(DIAS_PRUEBA))
                .build();
    }

    /**
     * @param administradores administradores ACTIVOS del negocio (0 = negocio sin administrador)
     * @param trabajadores    trabajadores ACTIVOS del negocio
     */
    public static NegocioResponseDTO toResponse(Negocio entidad, long administradores, long trabajadores) {
        return NegocioResponseDTO.builder()
                .idNegocio(entidad.getIdNegocio())
                .uuidNegocio(entidad.getUuidNegocio())
                .nombre(entidad.getNombre())
                .sistemaType(entidad.getSistemaType())
                .giro(mapearGiro(entidad.getGiro()))
                .correo(entidad.getCorreo())
                .telefono(entidad.getTelefono())
                .direccion(entidad.getDireccion())
                .municipio(mapearMunicipio(entidad.getMunicipio()))
                .razonSocial(entidad.getRazonSocial())
                .rfc(entidad.getRfc())
                .regimenFiscal(entidad.getRegimenFiscal())
                .codigoPostalFiscal(entidad.getCodigoPostalFiscal())
                .correoFacturacion(entidad.getCorreoFacturacion())
                .notasInternas(entidad.getNotasInternas())
                .plan(mapearPlan(entidad.getPlan()))
                .estatus(entidad.getEstatus())
                .fechaAlta(entidad.getFechaAlta())
                .vencimiento(entidad.getVencimiento())
                .logoUrl(construirUrlPublica(entidad.getLogoUrl()))
                .administradores(administradores)
                .trabajadores(trabajadores)
                .limiteAdministradores(entidad.getPlan().getMaxAdministradores())
                .limiteTrabajadores(entidad.getPlan().getMaxTrabajadores())
                .build();
    }

    private static CatalogoRefDTO mapearGiro(CatGiroNegocio giro) {
        return CatalogoRefDTO.builder()
                .id(giro.getIdGiro().intValue())
                .nombre(giro.getNombre())
                .build();
    }

    private static CatalogoRefDTO mapearPlan(Plan plan) {
        return CatalogoRefDTO.builder()
                .id(plan.getIdPlan().intValue())
                .nombre(plan.getNombre())
                .build();
    }

    // Convierte una ruta relativa guardada en BD a una URL pública completa y servible
    public static String construirUrlPublica(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) return null;
        return "/api/v1/archivos/ver/" + rutaRelativa;
    }

    // El municipio puede ser null en negocios registrados antes de que se pidiera este dato
    private static CatalogoRefDTO mapearMunicipio(CatMunicipio municipio) {
        if (municipio == null) return null;
        return CatalogoRefDTO.builder()
                .id(municipio.getIdMunicipio().intValue())
                .nombre(municipio.getNombre())
                .build();
    }

    /**
     * Copia los datos de facturación y las notas internas del DTO a un negocio que ya existe
     * (al editar). Al crear se asignan directamente en toEntity().
     */
    public static void aplicarDatosComplementarios(Negocio entidad, NegocioRequestDTO dto) {
        entidad.setRazonSocial(dto.getRazonSocial());
        entidad.setRfc(dto.getRfc());
        entidad.setRegimenFiscal(dto.getRegimenFiscal());
        entidad.setCodigoPostalFiscal(dto.getCodigoPostalFiscal());
        entidad.setCorreoFacturacion(dto.getCorreoFacturacion());
        entidad.setNotasInternas(dto.getNotasInternas());
    }
}
