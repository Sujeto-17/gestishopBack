package mx.com.gestishop.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos del trabajador que son iguales al dar de alta y al actualizar.
 * Los opcionales llegan como null (nunca cadena vacía): los @Pattern solo se evalúan si hay valor.
 */
@Getter
@Setter
public abstract class TrabajadorBaseDTO {

    // --- Datos personales ---
    @NotBlank
    @Size(min = 3, max = 100)
    @Schema(example = "Juan López García")
    private String nombre;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener 10 dígitos")
    @Schema(example = "9931000040", nullable = true)
    private String telefono;

    @Schema(nullable = true)
    private LocalDate fechaNacimiento;

    @Schema(description = "ID del género (catálogo)", nullable = true)
    private Long idGenero;

    @Schema(description = "ID del estado civil (catálogo)", nullable = true)
    private Long idEstadoCivil;

    @Schema(description = "ID del municipio de domicilio (catálogo)", nullable = true)
    private Long idMunicipio;

    @Size(max = 200)
    @Schema(nullable = true)
    private String direccion;

    @Pattern(regexp = "^[A-Z0-9]{18}$", message = "La CURP debe tener 18 caracteres en mayúsculas")
    @Schema(nullable = true)
    private String curp;

    @Pattern(regexp = "^\\d{11}$", message = "El NSS debe tener 11 dígitos")
    @Schema(nullable = true)
    private String nss;

    // --- Contacto de emergencia ---
    @Size(max = 100)
    @Schema(nullable = true)
    private String contactoEmergenciaNombre;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono de emergencia debe tener 10 dígitos")
    @Schema(nullable = true)
    private String contactoEmergenciaTelefono;

    @Size(max = 30)
    @Schema(example = "Madre", nullable = true)
    private String contactoEmergenciaParentesco;

    // --- Datos laborales ---
    @NotBlank @Size(max = 100)
    @Schema(example = "Operador de sublimación")
    private String puesto;

    @NotNull(message = "Debes elegir la tolerancia de retardo")
    private Long idTolerancia;

    @NotBlank
    @Pattern(regexp = "^(gerente|estandar)$", message = "El nivel de acceso debe ser 'gerente' o 'estandar'")
    @Schema(allowableValues = {"gerente", "estandar"})
    private String nivelAcceso;

    @NotBlank @Size(max = 20)
    @Schema(description = "Texto libre", example = "Planta")
    private String tipoContrato;

    @Size(max = 300)
    @Schema(nullable = true)
    private String notas;

    // --- Pago ---
    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "Monto por periodo de pago", example = "1800.00")
    private BigDecimal salario;

    @NotBlank
    @Pattern(regexp = "^(diario|semanal|quincenal|mensual)$", message = "Periodicidad de pago inválida")
    private String periodicidadPago = "mensual";

    @NotBlank
    @Pattern(regexp = "^(efectivo|transferencia|cheque)$", message = "Método de pago inválido")
    private String metodoPago = "efectivo";

    @Size(max = 50)
    @Schema(description = "Obligatorio si el método de pago es transferencia", nullable = true)
    private String banco;

    @Pattern(regexp = "^\\d{18}$", message = "La CLABE debe tener 18 dígitos")
    @Schema(description = "Obligatoria si el método de pago es transferencia", nullable = true)
    private String clabe;

    // --- Régimen ---
    @NotBlank
    @Pattern(regexp = "^(informal|formal)$", message = "El régimen debe ser 'informal' o 'formal'")
    @Schema(description = "formal exige CURP, NSS, RFC, SDI y fecha de alta en el IMSS")
    private String regimen = "informal";

    @Pattern(regexp = "^[A-ZÑ&]{4}\\d{6}[A-Z0-9]{3}$", message = "RFC inválido (persona física, 13 caracteres)")
    @Schema(nullable = true)
    private String rfc;

    @Size(max = 20)
    @Schema(nullable = true)
    private String numeroEmpleado;

    @DecimalMin("0.0")
    @Schema(description = "Salario diario integrado (base de cotización al IMSS)", nullable = true)
    private BigDecimal salarioDiarioIntegrado;

    @Schema(nullable = true)
    private LocalDate fechaAltaImss;

    @Min(value = 15, message = "El mínimo legal de aguinaldo es 15 días")
    @Max(365)
    @Schema(example = "15", nullable = true)
    private Short diasAguinaldo;

    @Min(value = 25, message = "El mínimo legal de prima vacacional es 25%")
    @Max(100)
    @Schema(example = "25", nullable = true)
    private Short primaVacacionalPct;
}