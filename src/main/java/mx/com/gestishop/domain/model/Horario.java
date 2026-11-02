package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Horario de trabajo: un nombre y, por cada día que se trabaja, su entrada y salida.
 * negocio = null indica un horario GLOBAL (precargado, inmutable); con valor es un horario propio del negocio.
 * Las reglas de edición/cancelación (bloqueo al primer uso) viven en HorarioServiceImpl y, como respaldo, en triggers de la BD.
 */
@Entity
@Table(name = "horarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long idHorario;

    @Column(name = "uuid_horario", nullable = false, unique = true)
    private UUID uuidHorario;

    // null = horario global
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_negocio")
    private Negocio negocio;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo (cancelado)

    @Builder.Default
    @OneToMany(mappedBy = "horario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("diaSemana ASC")
    private List<HorarioDia> dias = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // true si es un horario global (precargado por el sistema)
    public boolean esGlobal() {
        return negocio == null;
    }

    // Agrega un día y lo enlaza con este horario (mantiene los dos lados de la relación)
    public void agregarDia(HorarioDia dia) {
        dia.setHorario(this);
        dias.add(dia);
    }

    @PrePersist
    protected void onCreate() {
        if (uuidHorario == null) uuidHorario = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
