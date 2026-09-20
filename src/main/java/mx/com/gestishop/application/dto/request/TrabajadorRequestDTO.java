package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

//Datos completos para dar de alta un trabajador: identidad de acceso (correo, password) + datos laborales
@Getter
@Setter
@Schema(name = "TrabajadorRequestDTO", description = "Alta completa de un trabajador (acceso + datos laborales)")
public class TrabajadorRequestDTO {

    // --- Identidad de acceso ---
    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Juan López García")
    private String nombre;

    @NotBlank
    @Email
    @Schema(example = "juan.lopez@thejulians.com")
    private String correo;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(example = "9931000040", nullable = true)
    private String telefono;

    @NotBlank @Size(min = 8)
    @Schema(description = "Contraseña generada por el sistema, se encripta antes de guardar", example = "Xk9#mPz2")
    private String password;

    // --- Datos laborales ---
    @NotBlank @Size(max = 100)
    @Schema(example = "Operador de sublimación")
    private String puesto;

    @NotBlank
    @Schema(example = "operador", allowableValues = {"operador", "diseñador", "cajero", "vendedor", "otro"})
    private String rol;

    @NotBlank
    @Schema(description = "Controla qué módulos puede ver en el sistema", example = "estandar",
            allowableValues = {"gerente", "estandar"})
    private String nivelAcceso;

    @NotBlank
    @Schema(example = "mañana", allowableValues = {"mañana", "tarde", "completo"})
    private String turno;

    @NotBlank
    @Schema(example = "planta", allowableValues = {"planta", "temporal", "honorarios"})
    private String tipoContrato;

    @NotNull
    @DecimalMin("0.0")
    @Schema(example = "7500.00")
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

    // --- Contexto ---
    @NotNull
    @Schema(description = "Negocio al que pertenece este trabajador")
    private Long idNegocio;
}
