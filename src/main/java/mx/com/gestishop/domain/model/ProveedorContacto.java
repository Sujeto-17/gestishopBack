package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Persona de contacto de un proveedor (vendedor, cobranza…). Debe tener al menos un medio de contacto;
 * un proveedor solo puede tener un contacto principal.
 */
@Entity
@Table(name = "proveedor_contactos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contacto")
    private Long idContacto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "puesto", length = 60)
    private String puesto;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "whatsapp", length = 15)
    private String whatsapp;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal;

    @Column(name = "notas", length = 200)
    private String notas;
}