package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Valor de un atributo (Rojo, CH, 500 ml).
 * Con negocio = null es un valor del sistema que ven todos; con negocio es propio de ese negocio.
 */
@Entity
@Table(name = "atributo_valores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtributoValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valor")
    private Long idValor;

    @Column(name = "uuid_valor", nullable = false, unique = true)
    private UUID uuidValor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atributo", nullable = false)
    private Atributo atributo;

    // null = valor del sistema
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_negocio")
    private Negocio negocio;

    @Column(name = "valor", nullable = false, length = 40)
    private String valor;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo

    // true si el valor lo creó un negocio (no es del catálogo del sistema)
    public boolean esPropio() {
        return negocio != null;
    }

    @PrePersist
    protected void onCreate() {
        if (uuidValor == null) uuidValor = UUID.randomUUID();
        if (orden == null) orden = 0;
        if (estatus == null) estatus = "activo";
    }
}

