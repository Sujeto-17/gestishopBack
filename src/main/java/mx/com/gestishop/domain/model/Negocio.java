package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "negocios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_negocio")
    private Long idNegocio;

    @Column(name = "uuid_negocio", nullable = false, unique = true)
    private UUID uuidNegocio;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "sistema_type", nullable = false, length = 20)
    private String sistemaType;  // servicio | tienda

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "direccion", length = 200)
    private String direccion;

    // --- Datos de facturación (todos opcionales; van completos o no va ninguno) ---
    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(name = "rfc", length = 13)
    private String rfc;

    @Column(name = "regimen_fiscal", length = 3)
    private String regimenFiscal;  // clave SAT de 3 dígitos, ej. 601, 612, 626

    @Column(name = "codigo_postal_fiscal", length = 5)
    private String codigoPostalFiscal;

    // Notas privadas del superadmin (acuerdos de pago, contacto...). Nunca se muestran al negocio.
    @Column(name = "notas_internas", length = 300)
    private String notasInternas;

    // --- CONFIGURACIÓN FISCAL (la define el admin del negocio) ---
    // false = el negocio no factura y el IVA no existe para él; true = emite factura
    @Builder.Default
    @Column(name = "emite_factura", nullable = false)
    private Boolean emiteFactura = false;

    // Solo aplica si emiteFactura: true = el precio público ya trae el IVA; false = se suma al facturar
    @Builder.Default
    @Column(name = "precios_incluyen_iva", nullable = false)
    private Boolean preciosIncluyenIva = true;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;  // activo | prueba | inactivo

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @Column(name = "vencimiento", nullable = false)
    private LocalDate vencimiento;

    // --- LIST OF TABLE ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_giro", nullable = false)
    private CatGiroNegocio giro;

    // Municipio donde se ubica (obligatorio al registrar; nullable en BD por los negocios que ya existían)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_municipio")
    private CatMunicipio municipio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan", nullable = false)
    private Plan plan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (uuidNegocio == null) uuidNegocio = UUID.randomUUID();
        if (fechaAlta == null) fechaAlta = LocalDate.now();
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
