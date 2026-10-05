package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Archivo adjunto genérico (foto, documento) perteneciente a cualquier
 * entidad del sistema. La relación hacia la entidad dueña es polimórfica
 * (idTipoEntidad + idEntidad), no una FK real de JPA/Postgres — la
 * integridad se gestiona desde ArchivoServiceImpl.
 */
@Entity
@Table(name = "archivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Archivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_archivo")
    private Long idArchivo;

    @Column(name = "uuid_archivo", nullable = false, unique = true)
    private UUID uuidArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_entidad", nullable = false)
    private CatTipoEntidadArchivo tipoEntidad;

    @Column(name = "id_entidad", nullable = false)
    private Long idEntidad;

    @Column(name = "categoria", nullable = false, length = 30)
    private String categoria;

    @Column(name = "nombre_archivo", nullable = false, length = 150)
    private String nombreArchivo;

    @Column(name = "nombre_original", length = 255)
    private String nombreOriginal;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "ruta_relativa", nullable = false, length = 255)
    private String rutaRelativa;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @Column(name = "estatus", nullable = false, length = 20)
    private String estatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (uuidArchivo == null) uuidArchivo = UUID.randomUUID();
        if (esPrincipal == null) esPrincipal = false;
        if (orden == null) orden = 0;
        if (estatus == null) estatus = "activo";
        createdAt = OffsetDateTime.now();
    }
}