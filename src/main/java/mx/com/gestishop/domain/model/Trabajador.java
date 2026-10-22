package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trabajadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trabajador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_trabajador")
    private Long idTrabajador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_negocio", nullable = false)
    private Negocio negocio;

    // --- Datos personales ---
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "curp", length = 18)
    private String curp;

    @Column(name = "nss", length = 11)
    private String nss;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_genero")
    private CatGenero genero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_civil")
    private CatEstadoCivil estadoCivil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_municipio")
    private CatMunicipio municipio;

    // --- Contacto de emergencia ---
    @Column(name = "contacto_emergencia_nombre", length = 100)
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono", length = 10)
    private String contactoEmergenciaTelefono;

    @Column(name = "contacto_emergencia_parentesco", length = 30)
    private String contactoEmergenciaParentesco;

    // --- Datos laborales ---
    @Column(name = "puesto", nullable = false, length = 100)
    private String puesto;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol;  // texto libre: lo que hace en ese negocio

    @Column(name = "nivel_acceso", nullable = false, length = 20)
    private String nivelAcceso;  // gerente | estandar — controla qué módulos ve

    @Column(name = "turno", nullable = false, length = 20)
    private String turno;

    @Column(name = "tipo_contrato", nullable = false, length = 20)
    private String tipoContrato;  // texto libre

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "notas", length = 300)
    private String notas;

    // --- Pago ---
    @Column(name = "salario", nullable = false, precision = 10, scale = 2)
    private BigDecimal salario;  // monto por periodo de pago

    @Column(name = "periodicidad_pago", nullable = false, length = 20)
    private String periodicidadPago;  // diario | semanal | quincenal | mensual

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;  // efectivo | transferencia | cheque

    @Column(name = "banco", length = 50)
    private String banco;

    @Column(name = "clabe", length = 18)
    private String clabe;

    // --- Régimen ---
    @Column(name = "regimen", nullable = false, length = 10)
    private String regimen;  // informal | formal

    @Column(name = "rfc", length = 13)
    private String rfc;

    @Column(name = "numero_empleado", length = 20)
    private String numeroEmpleado;

    @Column(name = "salario_diario_integrado", precision = 10, scale = 2)
    private BigDecimal salarioDiarioIntegrado;

    @Column(name = "fecha_alta_imss")
    private LocalDate fechaAltaImss;

    @Column(name = "dias_aguinaldo")
    private Short diasAguinaldo;

    @Column(name = "prima_vacacional_pct")
    private Short primaVacacionalPct;

    @PrePersist
    protected void onCreate() {
        if (fechaIngreso == null) fechaIngreso = LocalDate.now();
    }
}