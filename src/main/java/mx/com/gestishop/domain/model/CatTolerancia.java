package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Catálogo general y oficial de tolerancias de retardo (0, 5, 10... 30 minutos)
 * No lo edita nadie desde la aplicación; se elige al asignar un horario al trabajador
 */
@Entity
@Table(name = "cat_tolerancias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatTolerancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tolerancia")
    private Long idTolerancia;

    @Column(name = "minutos", nullable = false, unique = true)
    private Short minutos;   // 0 = sin tolerancia

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;   // 'Sin tolerancia', '15 minutos'...
}
