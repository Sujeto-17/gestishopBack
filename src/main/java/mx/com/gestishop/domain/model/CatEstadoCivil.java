package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_estados_civiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatEstadoCivil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_civil")
    private Long idEstadoCivil;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;
}
