package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import mx.com.gestishop.core.dto.CatalogoRefDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Representación completa de un trabajador, uniendo sus datos de acceso y laborales
@Getter
@Builder
@Schema(name = "TrabajadorResponseDTO", description = "Trabajador con datos de acceso, laborales y de nómina")
public class TrabajadorResponseDTO {

    private Long idTrabajador;
    private UUID uuidUsuario;
    private Long idNegocio;

    // Acceso
    private String nombre;
    private String correo;
    private String telefono;
    private String estatus;

    // Personales
    private LocalDate fechaNacimiento;
    private String direccion;
    private String curp;
    private String nss;
    private CatalogoRefDTO genero;
    private CatalogoRefDTO estadoCivil;
    private CatalogoRefDTO municipio;

    // Contacto de emergencia
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String contactoEmergenciaParentesco;

    // Laborales
    private String puesto;
    private Long idTolerancia;
    private Integer toleranciaMinutos;
    private String toleranciaNombre;
    private String nivelAcceso;
    private String tipoContrato;
    private LocalDate fechaIngreso;
    private String notas;

    // Pago
    private BigDecimal salario;
    private String periodicidadPago;
    private String metodoPago;
    private String banco;
    private String clabe;

    // Régimen
    private String regimen;
    private String rfc;
    private String numeroEmpleado;
    private BigDecimal salarioDiarioIntegrado;
    private LocalDate fechaAltaImss;
    private Short diasAguinaldo;
    private Short primaVacacionalPct;
}