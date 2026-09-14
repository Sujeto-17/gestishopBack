package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_giros_negocio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatGiroNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_giro")
    private Long idGiro;

    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(name = "icono", length = 30)
    private String icono;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;
}
