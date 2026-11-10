package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Atributo global de variante (Color, Talla, Capacidad...).
 * Lo administra el superadmin y lo comparten todos los negocios.
 */
@Entity
@Table(name = "atributos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atributo")
    private Long idAtributo;

    @Column(name = "uuid_atributo", nullable = false, unique = true)
    private UUID uuidAtributo;

    @Column(name = "nombre", nullable = false, length = 40)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;   // activo | inactivo

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (uuidAtributo == null) uuidAtributo = UUID.randomUUID();
        if (orden == null) orden = 0;
        if (estatus == null) estatus = "activo";
        createdAt = OffsetDateTime.now();
    }
}