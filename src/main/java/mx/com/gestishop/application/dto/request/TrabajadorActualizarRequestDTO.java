package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(name = "TrabajadorActualizarRequestDTO", description = "Actualización de datos de un trabajador (sin contraseña)")
public class TrabajadorActualizarRequestDTO {

    @NotBlank
    @Size(min = 3, max = 100)
    private String nombre;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    private String telefono;

    @Schema(nullable = true)
    private LocalDate fechaNacimiento;

    @Size(max = 200)
    @Schema(nullable = true)
    private String direccion;

    @NotBlank @Size(max = 100)
    private String puesto;

    @NotBlank
    private String rol;

    @NotBlank
    private String nivelAcceso;

    @NotBlank
    private String turno;

    @NotBlank
    private String tipoContrato;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal salario;

    @Size(max = 18)
    @Schema(nullable = true)
    private String curp;

    @Size(max = 11)
    @Schema(nullable = true)
    private String nss;

    @Size(max = 300)
    @Schema(nullable = true)
    private String notas;
}
