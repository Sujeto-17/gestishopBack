package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

// Catálogo general de unidades de medida (pieza, metro, kilogramo, litro...)
@Entity
@Table(name = "cat_unidades_medida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatUnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Long idUnidad;

    @Column(name = "clave", nullable = false, unique = true, length = 10)
    private String clave;   // 'pza', 'm', 'kg'

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String nombre;

    // true en unidades que se pueden fraccionar (metro, litro, kilo); false en piezas
    @Column(name = "permite_decimales", nullable = false)
    private Boolean permiteDecimales;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;
}