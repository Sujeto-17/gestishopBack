package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import mx.com.gestishop.core.dto.CatalogoRefDTO;

import java.time.LocalDate;
import java.util.UUID;

// Representación completa de un negocio registrado en la plataforma
@Getter
@Builder
@Schema(name = "NegocioResponseDTO", description = "Negocio con todos sus datos")
public class NegocioResponseDTO {

    @Schema(description = "Identificador interno")
    private Long idNegocio;

    @Schema(description = "Identificador público")
    private UUID uuidNegocio;

    @Schema(description = "Nombre del negocio")
    private String nombre;

    @Schema(description = "URL del logo", nullable = true)
    private String logoUrl;

    @Schema(description = "Tipo de flujo operativo", example = "servicio")
    private String sistemaType;

    @Schema(description = "Giro comercial")
    private CatalogoRefDTO giro;

    @Schema(description = "Correo de contacto")
    private String correo;

    @Schema(description = "Teléfono", nullable = true)
    private String telefono;

    @Schema(description = "Dirección", nullable = true)
    private String direccion;

    @Schema(description = "Plan de suscripción actual")
    private CatalogoRefDTO plan;

    @Schema(description = "Estatus del negocio", example = "prueba")
    private String estatus;

    @Schema(description = "Fecha de alta")
    private LocalDate fechaAlta;

    @Schema(description = "Fecha de vencimiento del plan/prueba actual")
    private LocalDate vencimiento;

    @Schema(description = "Municipio donde se ubica el negocio", nullable = true)
    private CatalogoRefDTO municipio;

    @Schema(description = "Razón social", nullable = true)
    private String razonSocial;

    @Schema(description = "RFC", nullable = true)
    private String rfc;

    @Schema(description = "Clave del régimen fiscal (SAT)", nullable = true)
    private String regimenFiscal;

    @Schema(description = "Código postal fiscal", nullable = true)
    private String codigoPostalFiscal;

    @Schema(description = "true si el negocio emite factura")
    private Boolean emiteFactura;

    @Schema(description = "Solo aplica si emite factura: true = los precios ya incluyen IVA")
    private Boolean preciosIncluyenIva;

    @Schema(description = "Notas internas del superadmin", nullable = true)
    private String notasInternas;

    @Schema(description = "Administradores ACTIVOS del negocio. 0 = negocio sin administrador (hay que asignarle uno)")
    private Long administradores;

    @Schema(description = "Trabajadores ACTIVOS del negocio")
    private Long trabajadores;

    @Schema(description = "Límite de administradores de su plan; null = sin límite", nullable = true)
    private Integer limiteAdministradores;

    @Schema(description = "Límite de trabajadores de su plan; null = sin límite", nullable = true)
    private Integer limiteTrabajadores;
}
