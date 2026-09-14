package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "planes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan")
    private Long idPlan;

    @Column(name = "uuid_plan", nullable = false, unique = true)
    private UUID uuidPlan;

    @Column(name = "nivel", nullable = false, length = 20)
    private String nivel;   // basico | pro | elite

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "sistema_type", nullable = false, length = 20)
    private String sistemaType;  // servicio | tienda

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "descripcion", nullable = false, length = 160)
    private String descripcion;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "plan_modulos",
            joinColumns = @JoinColumn(name = "id_plan"),
            inverseJoinColumns = @JoinColumn(name = "id_modulo")
    )
    private Set<CatModulo> modulos = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (uuidPlan == null) uuidPlan = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
