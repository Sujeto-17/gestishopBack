package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Proveedor de un negocio: a quién se le compra, en qué condiciones y con qué contactos.
 * Cada negocio maneja los suyos; solo el admin tiene acceso (el gerente nunca los ve).
 */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "uuid_proveedor", nullable = false, unique = true)
    private UUID uuidProveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_negocio", nullable = false)
    private Negocio negocio;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(name = "rfc", length = 13)
    private String rfc;

    // Qué vende, de un vistazo: "papelería, vinil, sublimación"
    @Column(name = "resumen_surtido", length = 200)
    private String resumenSurtido;

    // --- Facturación ---
    @Column(name = "factura", nullable = false)
    private Boolean factura;

    // true = sus precios ya traen IVA; false = el IVA se suma al facturar
    @Column(name = "precios_incluyen_iva", nullable = false)
    private Boolean preciosIncluyenIva;

    // --- Envíos ---
    @Column(name = "hace_envios", nullable = false)
    private Boolean haceEnvios;

    @Column(name = "costo_envio", precision = 10, scale = 2)
    private BigDecimal costoEnvio;

    @Column(name = "envio_gratis_desde", precision = 10, scale = 2)
    private BigDecimal envioGratisDesde;

    // --- Condiciones de compra ---
    @Column(name = "pedido_minimo", precision = 10, scale = 2)
    private BigDecimal pedidoMinimo;

    @Column(name = "formas_pago", length = 100)
    private String formasPago;

    // Apreciación interna de 1 a 5
    @Column(name = "calificacion")
    private Short calificacion;

    // --- Ubicación y notas ---
    @Column(name = "direccion", length = 200)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_municipio")
    private CatMunicipio municipio;

    @Column(name = "sitio_web", length = 150)
    private String sitioWeb;

    @Column(name = "notas", length = 500)
    private String notas;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("esPrincipal DESC, nombre ASC")
    @Builder.Default
    private List<ProveedorContacto> contactos = new ArrayList<>();

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tipo ASC, montoMinimo ASC")
    @Builder.Default
    private List<ProveedorDescuento> descuentos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (uuidProveedor == null) uuidProveedor = UUID.randomUUID();
        if (estatus == null) estatus = "activo";
        if (factura == null) factura = false;
        if (preciosIncluyenIva == null) preciosIncluyenIva = true;
        if (haceEnvios == null) haceEnvios = false;
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
