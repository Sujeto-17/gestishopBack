package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ficha general de un producto de venta. Lo que se vende y se cuenta son sus variantes
 * (un producto sin atributos tiene una sola variante).
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "uuid_producto", nullable = false, unique = true)
    private UUID uuidProducto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_negocio", nullable = false, updatable = false)
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "marca", length = 60)
    private String marca;

    @Column(name = "modelo", length = 60)
    private String modelo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    private CatUnidadMedida unidad;

    @Column(name = "tasa_iva", nullable = false)
    private Short tasaIva;   // 0 | 8 | 16

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (uuidProducto == null) uuidProducto = UUID.randomUUID();
        if (tasaIva == null) tasaIva = 16;
        if (estatus == null) estatus = "activo";
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
