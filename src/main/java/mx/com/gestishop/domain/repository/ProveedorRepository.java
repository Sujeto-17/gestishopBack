package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de proveedores. Todas las consultas filtran por negocio: nunca se ve un proveedor de otro negocio
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Proveedores del negocio con municipio y contactos ya cargados (los descuentos no se necesitan en la lista)
    @Query("""
            SELECT DISTINCT p FROM Proveedor p
            LEFT JOIN FETCH p.municipio
            LEFT JOIN FETCH p.contactos
            WHERE p.negocio.idNegocio = :idNegocio
            ORDER BY p.nombre ASC
            """)
    List<Proveedor> listarPorNegocio(@Param("idNegocio") Long idNegocio);

    // Un proveedor por su identificador público, solo si es del negocio (los descuentos se cargan al leerlos)
    @Query("""
            SELECT p FROM Proveedor p
            LEFT JOIN FETCH p.municipio
            LEFT JOIN FETCH p.contactos
            WHERE p.uuidProveedor = :uuid AND p.negocio.idNegocio = :idNegocio
            """)
    Optional<Proveedor> buscarPorUuid(@Param("uuid") UUID uuid, @Param("idNegocio") Long idNegocio);

    // ¿Existe otro proveedor con ese nombre (sin distinguir mayúsculas)? excluirId evita compararse consigo mismo
    @Query("""
            SELECT COUNT(p) > 0 FROM Proveedor p
            WHERE p.negocio.idNegocio = :idNegocio AND lower(p.nombre) = lower(:nombre)
              AND p.idProveedor <> :excluirId
            """)
    boolean existeNombre(@Param("idNegocio") Long idNegocio, @Param("nombre") String nombre,
                         @Param("excluirId") Long excluirId);

    // Cuántas variantes surte cada proveedor del negocio: filas [idProveedor, cantidad]
    @Query(value = """
            SELECT id_proveedor, COUNT(*) FROM variante_proveedores
            WHERE id_negocio = :idNegocio
            GROUP BY id_proveedor
            """, nativeQuery = true)
    List<Object[]> contarVariantesPorProveedor(@Param("idNegocio") Long idNegocio);

    // Movimientos de todo tipo que referencian al proveedor (costos de variantes, solicitudes de compra y entradas).
    // Si hay alguno ya no se puede eliminar: solo desactivar, para no perder el historial.
    @Query(value = """
            SELECT (SELECT COUNT(*) FROM variante_proveedores   WHERE id_proveedor = :idProveedor)
                 + (SELECT COUNT(*) FROM solicitudes_compra     WHERE id_proveedor = :idProveedor)
                 + (SELECT COUNT(*) FROM movimientos_inventario WHERE id_proveedor = :idProveedor)
            """, nativeQuery = true)
    long contarReferencias(@Param("idProveedor") Long idProveedor);
}
