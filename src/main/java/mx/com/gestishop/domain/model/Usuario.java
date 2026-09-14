package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "uuid_usuario", nullable = false, unique = true)
    private UUID uuidUsuario;

    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private String tipoUsuario;  // superadmin | admin | trabajador

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;  // ALWAYS encrypted with BCrypt, never plain text

    @Column(name = "debe_actualizar_password", nullable = false)
    private Boolean debeActualizarPassword;

    @Column(name = "ultimo_acceso")
    private OffsetDateTime ultimoAcceso;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    // --- LIST OF TABLES ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_genero")
    private CatGenero genero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_civil")
    private CatEstadoCivil estadoCivil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_municipio")
    private CatMunicipio municipio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (uuidUsuario == null) uuidUsuario = UUID.randomUUID();
        if (fechaAlta == null) fechaAlta = LocalDate.now();
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
