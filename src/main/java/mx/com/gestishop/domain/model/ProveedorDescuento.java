package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Descuento de un proveedor (informativo). Tipos:
 *  - mensual:    por compra mensual acumulada ("comprando $10,000 al mes, 5 %")
 *  - por_compra: por monto de una sola compra ("en un pedido desde $3,000, 3 %")
 *  - fijo:       siempre aplica, sin monto ("cliente frecuente, 2 %")
 */
@Entity
@Table(name = "proveedor_descuentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_descuento")
    private Long idDescuento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    // mensual | por_compra | fijo
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    // Monto a partir del cual aplica; null solo en el tipo fijo
    @Column(name = "monto_minimo", precision = 12, scale = 2)
    private BigDecimal montoMinimo;

    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "descripcion", length = 150)
    private String descripcion;
}
