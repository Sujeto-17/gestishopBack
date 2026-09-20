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
}
