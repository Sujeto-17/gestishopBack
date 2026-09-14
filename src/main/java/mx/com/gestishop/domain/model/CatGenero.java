package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_generos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatGenero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_genero")
    private Long idGenero;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;
}
