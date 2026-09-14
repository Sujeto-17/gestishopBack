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

    @Column(name = "curp", length = 18)
    private String curp;

    @Column(name = "nss", length = 11)
    private String nss;

    @Column(name = "puesto", nullable = false, length = 100)
    private String puesto;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol;  // operador | diseñador | cajero | vendedor | otro

    @Column(name = "nivel_acceso", nullable = false, length = 20)
    private String nivelAcceso;  // gerente | estandar — controla qué módulos ve

    @Column(name = "turno", nullable = false, length = 20)
    private String turno;

    @Column(name = "tipo_contrato", nullable = false, length = 20)
    private String tipoContrato;

    @Column(name = "salario", nullable = false, precision = 10, scale = 2)
    private BigDecimal salario;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "notas", length = 300)
    private String notas;
}
