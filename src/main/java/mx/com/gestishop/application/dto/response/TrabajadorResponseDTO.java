package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Representación completa de un trabajador, uniendo sus datos de acceso y laborales
@Getter
@Builder
@Schema(name = "TrabajadorResponseDTO", description = "Trabajador con datos de acceso y laborales")
public class TrabajadorResponseDTO {

    @Schema(description = "Identificador interno del trabajador")
    private Long idTrabajador;

    @Schema(description = "Identificador público del usuario asociado")
    private UUID uuidUsuario;

    @Schema(example = "Juan López García")
    private String nombre;

    @Schema(example = "juan.lopez@thejulians.com")
    private String correo;

    @Schema(example = "9931000040", nullable = true)
    private String telefono;

    @Schema(example = "Operador de sublimación")
    private String puesto;

    @Schema(example = "operador")
    private String rol;

    @Schema(example = "estandar")
    private String nivelAcceso;

    @Schema(example = "mañana")
    private String turno;

    @Schema(example = "planta")
    private String tipoContrato;

    @Schema(example = "7500.00")
    private BigDecimal salario;

    @Schema(nullable = true)
    private String curp;

    @Schema(nullable = true)
    private String nss;

    @Schema(nullable = true)
    private String notas;

    @Schema(description = "Estatus de la cuenta de acceso", example = "activo")
    private String estatus;

    @Schema(description = "Fecha de ingreso laboral")
    private LocalDate fechaIngreso;

    @Schema(description = "ID del negocio al que pertenece")
    private Long idNegocio;
}