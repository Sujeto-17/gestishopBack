package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol_modulos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tipo_usuario", "nivel_acceso", "id_modulo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_modulo")
    private Long idRolModulo;

    @Column(name = "tipo_usuario", nullable = false, length = 20)
    private String tipoUsuario;  // admin | trabajador

    @Column(name = "nivel_acceso", length = 20)
    private String nivelAcceso;  // gerente | estandar | null (si tipo_usuario=admin)

    // --- LIST OF TABLES ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modulo", nullable = false)
    private CatModulo modulo;
}
