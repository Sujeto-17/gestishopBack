package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Quién surte una variante y a qué costo. Una fila por variante y proveedor; solo una puede ser la preferida.
 * Es información privada del admin: el gerente nunca la recibe.
 * El historial de cambios de costo lo registra un trigger de la BD (variante_costos_historial).
 */
@Entity
@Table(name = "variante_proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VarianteProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variante_proveedor")
    private Long idVarianteProveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false, updatable = false)
    private Variante variante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false, updatable = false)
    private Proveedor proveedor;

    @Column(name = "id_negocio", nullable = false, updatable = false)
    private Long idNegocio;

    // Costo tal como lo cotiza el proveedor (ver Proveedor.preciosIncluyenIva)
    @Column(name = "costo", nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    // Clave que el proveedor usa para este artículo
    @Column(name = "codigo_proveedor", length = 40)
    private String codigoProveedor;

    @Column(name = "es_preferido", nullable = false)
    private Boolean esPreferido;

    @Column(name = "notas", length = 200)
    private String notas;

    @Column(name = "ultima_compra")
    private LocalDate ultimaCompra;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Valores por defecto al insertar
    @PrePersist
    protected void onCreate() {
        if (esPreferido == null) esPreferido = false;
        if (estatus == null) estatus = "activo";
        updatedAt = OffsetDateTime.now();
    }

    // Marca de última modificación
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

