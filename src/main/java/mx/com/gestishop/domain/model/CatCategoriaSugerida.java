package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Plantilla de categoría que se ofrece a los negocios de un giro. El admin elige cuáles copiar
 * a sus categorías; la copia es independiente (editarla no modifica la plantilla).
 */
@Entity
@Table(name = "cat_categorias_sugeridas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatCategoriaSugerida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sugerida")
    private Long idSugerida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_giro", nullable = false)
    private CatGiroNegocio giro;

    // null = familia; con valor = subfamilia de esa familia
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre")
    private CatCategoriaSugerida padre;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Short orden;
}