package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cat_tipos_entidad_archivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatTipoEntidadArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_entidad")
    private Long idTipoEntidad;

    @Column(name = "clave", nullable = false, unique = true, length = 30)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;
}
