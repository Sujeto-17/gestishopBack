package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Unidad real de venta e inventario de un producto (por ejemplo, "taza roja").
 * El stock NO se edita desde aquí: la BD lo calcula con los movimientos de inventario,
 * por eso la columna es de solo lectura.
 */
@Entity
@Table(name = "variantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variante")
    private Long idVariante;

    @Column(name = "uuid_variante", nullable = false, unique = true)
    private UUID uuidVariante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false, updatable = false)
    private Producto producto;

    @Column(name = "id_negocio", nullable = false, updatable = false)
    private Long idNegocio;

    @Column(name = "sku", length = 40)
    private String sku;

    @Column(name = "codigo_barras", length = 30)
    private String codigoBarras;

    // Combinación de valores en forma canónica ("3:12|5:40"); vacía en un producto simple
    @Column(name = "firma", nullable = false, length = 200, updatable = false)
    private String firma;

    @Column(name = "precio_publico", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPublico;

    @Column(name = "precio_mayoreo", precision = 12, scale = 2)
    private BigDecimal precioMayoreo;

    @Column(name = "cantidad_mayoreo", precision = 12, scale = 3)
    private BigDecimal cantidadMayoreo;

    // Solo lectura: lo modifica el trigger de movimientos
    @Column(name = "stock_actual", insertable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal stockActual;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (uuidVariante == null) uuidVariante = UUID.randomUUID();
        if (firma == null) firma = "";
        if (stockMinimo == null) stockMinimo = BigDecimal.ZERO;
        if (estatus == null) estatus = "activo";
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
