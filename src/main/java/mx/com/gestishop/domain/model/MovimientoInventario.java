package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Movimiento de inventario: única forma de cambiar el stock de una variante.
 * Es inmutable; el stock anterior y el resultante los llena el trigger de la BD.
 */
@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @Column(name = "uuid_movimiento", nullable = false, unique = true, updatable = false)
    private UUID uuidMovimiento;

    @Column(name = "id_negocio", nullable = false, updatable = false)
    private Long idNegocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false, updatable = false)
    private Variante variante;

    @Column(name = "tipo", nullable = false, length = 10, updatable = false)
    private String tipo;     // entrada | salida | ajuste

    @Column(name = "motivo", nullable = false, length = 25, updatable = false)
    private String motivo;   // inventario_inicial, compra, venta...

    // Con signo: entrada > 0, salida < 0
    @Column(name = "cantidad", nullable = false, precision = 12, scale = 3, updatable = false)
    private BigDecimal cantidad;

    // Los llena el trigger de la BD
    @Column(name = "stock_anterior", insertable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal stockAnterior;

    @Column(name = "stock_resultante", insertable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal stockResultante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    // Proveedor de la compra (solo en resurtidos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", updatable = false)
    private Proveedor proveedor;

    // Privado del admin; puede quedar vacío y completarse después (la BD solo permite hacerlo una vez, con UPDATE directo)
    @Column(name = "costo_unitario", precision = 12, scale = 2, updatable = false)
    private BigDecimal costoUnitario;

    @Column(name = "notas", length = 300, updatable = false)
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (uuidMovimiento == null) uuidMovimiento = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
    }
}
