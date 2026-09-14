package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_estados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idEstado;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "clave_inegi", length = 2)
    private String claveInegi;
}
