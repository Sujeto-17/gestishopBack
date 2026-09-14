package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_municipios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatMunicipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_municipio")
    private Long idMunicipio;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    // --- LIST OF TABLES ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = false)
    private CatEstado estado;
}
