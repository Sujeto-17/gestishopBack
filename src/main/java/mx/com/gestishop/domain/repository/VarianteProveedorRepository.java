package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.VarianteProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Costos por variante y proveedor; siempre acotados al negocio
public interface VarianteProveedorRepository extends JpaRepository<VarianteProveedor, Long> {

    // Todos los costos de las variantes de un producto, con el proveedor cargado; el preferido va primero
    @Query("""
            SELECT vp FROM VarianteProveedor vp
            JOIN FETCH vp.proveedor
            WHERE vp.variante.producto.idProducto = :idProducto AND vp.idNegocio = :idNegocio
            ORDER BY vp.esPreferido DESC, vp.costo ASC, vp.idVarianteProveedor ASC
            """)
    List<VarianteProveedor> listarPorProducto(@Param("idProducto") Long idProducto, @Param("idNegocio") Long idNegocio);

    // Costos de una sola variante; el preferido va primero
    @Query("""
            SELECT vp FROM VarianteProveedor vp
            JOIN FETCH vp.proveedor
            WHERE vp.variante.idVariante = :idVariante
            ORDER BY vp.esPreferido DESC, vp.costo ASC, vp.idVarianteProveedor ASC
            """)
    List<VarianteProveedor> listarPorVariante(@Param("idVariante") Long idVariante);

    // El costo de una variante con un proveedor concreto
    @Query("""
            SELECT vp FROM VarianteProveedor vp
            WHERE vp.variante.idVariante = :idVariante AND vp.proveedor.idProveedor = :idProveedor
            """)
    Optional<VarianteProveedor> buscar(@Param("idVariante") Long idVariante, @Param("idProveedor") Long idProveedor);

    // Cuántos proveedores tiene registrados la variante
    @Query("SELECT COUNT(vp) FROM VarianteProveedor vp WHERE vp.variante.idVariante = :idVariante")
    long contarPorVariante(@Param("idVariante") Long idVariante);

    // Quita la marca de preferido de la variante (antes de marcar otro; el índice único permite solo uno).
    // Vacía la caché de persistencia para no dejar entidades desactualizadas.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE VarianteProveedor vp SET vp.esPreferido = false WHERE vp.variante.idVariante = :idVariante AND vp.esPreferido = true")
    void quitarPreferido(@Param("idVariante") Long idVariante);

    // Últimos cambios de costo de la variante: filas [fecha, uuid_proveedor, proveedor, costo_anterior, costo_nuevo]
    @Query(value = """
            SELECT h.created_at, p.uuid_proveedor, p.nombre, h.costo_anterior, h.costo_nuevo
            FROM variante_costos_historial h
            JOIN proveedores p ON p.id_proveedor = h.id_proveedor
            WHERE h.id_variante = :idVariante AND h.id_negocio = :idNegocio
            ORDER BY h.created_at DESC, h.id_historial DESC
            LIMIT 50
            """, nativeQuery = true)
    List<Object[]> historial(@Param("idVariante") Long idVariante, @Param("idNegocio") Long idNegocio);
}

