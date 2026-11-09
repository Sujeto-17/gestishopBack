package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.ProveedorContactoResponseDTO;
import mx.com.gestishop.application.dto.response.ProveedorDescuentoResponseDTO;
import mx.com.gestishop.application.dto.response.ProveedorResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.domain.model.Proveedor;
import mx.com.gestishop.domain.model.ProveedorContacto;
import mx.com.gestishop.domain.model.ProveedorDescuento;

import java.util.List;

// Convierte entre entidades de proveedores y sus DTOs
public class ProveedorMapper {

    private ProveedorMapper() {
    }

    // Respuesta de un proveedor. Los descuentos solo se incluyen en el detalle para no cargarlos uno por uno en la lista
    public static ProveedorResponseDTO toResponse(Proveedor p, int totalVariantes, boolean incluirDescuentos) {
        return ProveedorResponseDTO.builder()
                .uuidProveedor(p.getUuidProveedor())
                .nombre(p.getNombre())
                .razonSocial(p.getRazonSocial())
                .rfc(p.getRfc())
                .resumenSurtido(p.getResumenSurtido())
                .factura(Boolean.TRUE.equals(p.getFactura()))
                .preciosIncluyenIva(!Boolean.FALSE.equals(p.getPreciosIncluyenIva()))
                .haceEnvios(Boolean.TRUE.equals(p.getHaceEnvios()))
                .costoEnvio(p.getCostoEnvio())
                .envioGratisDesde(p.getEnvioGratisDesde())
                .pedidoMinimo(p.getPedidoMinimo())
                .formasPago(p.getFormasPago())
                .calificacion(p.getCalificacion())
                .direccion(p.getDireccion())
                .municipio(p.getMunicipio() == null ? null : CatalogoRefDTO.builder()
                        .id(p.getMunicipio().getIdMunicipio().intValue())
                        .nombre(p.getMunicipio().getNombre())
                        .build())
                .sitioWeb(p.getSitioWeb())
                .notas(p.getNotas())
                .estatus(p.getEstatus())
                .totalVariantes(totalVariantes)
                .contactos(p.getContactos().stream().map(ProveedorMapper::toResponse).toList())
                .descuentos(incluirDescuentos
                        ? p.getDescuentos().stream().map(ProveedorMapper::toResponse).toList()
                        : List.of())
                .build();
    }

    private static ProveedorContactoResponseDTO toResponse(ProveedorContacto c) {
        return ProveedorContactoResponseDTO.builder()
                .nombre(c.getNombre())
                .puesto(c.getPuesto())
                .telefono(c.getTelefono())
                .whatsapp(c.getWhatsapp())
                .correo(c.getCorreo())
                .esPrincipal(Boolean.TRUE.equals(c.getEsPrincipal()))
                .notas(c.getNotas())
                .build();
    }

    private static ProveedorDescuentoResponseDTO toResponse(ProveedorDescuento d) {
        return ProveedorDescuentoResponseDTO.builder()
                .tipo(d.getTipo())
                .montoMinimo(d.getMontoMinimo())
                .porcentaje(d.getPorcentaje())
                .descripcion(d.getDescripcion())
                .build();
    }
}
