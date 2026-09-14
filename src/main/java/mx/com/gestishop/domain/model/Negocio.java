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
