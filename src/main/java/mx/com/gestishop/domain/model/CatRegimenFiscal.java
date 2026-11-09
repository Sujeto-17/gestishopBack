package mx.com.gestishop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

// Catálogo oficial de regímenes fiscales del SAT
@Entity
@Table(name = "cat_regimenes_fiscales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatRegimenFiscal {

    @Id
    @Column(name = "clave", length = 3)
    private String clave;   // '601', '612', '626'...

    @Column(name = "descripcion", nullable = false, length = 150)
    private String descripcion;

    // true si el régimen aplica a personas físicas (RFC de 13 caracteres)
    @Column(name = "persona_fisica", nullable = false)
    private Boolean personaFisica;

    // true si el régimen aplica a personas morales (RFC de 12 caracteres)
    @Column(name = "persona_moral", nullable = false)
    private Boolean personaMoral;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;
}
