package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Atributo;
import mx.com.gestishop.domain.model.AtributoValor;
import mx.com.gestishop.domain.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de productos. Todas las consultas filtran por negocio: nunca se ve un producto de otro negocio
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Productos del negocio con categoría y unidad ya cargadas
    @Query("""
            SELECT p FROM Producto p
            JOIN FETCH p.categoria JOIN FETCH p.unidad
            WHERE p.negocio.idNegocio = :idNegocio
              AND (:soloActivos = false OR p.estatus = 'activo')
            ORDER BY lower(p.nombre) ASC
            """)
    List<Producto> listarPorNegocio(@Param("idNegocio") Long idNegocio, @Param("soloActivos") boolean soloActivos);

    // Un producto por su identificador público, solo si es del negocio
    @Query("""
            SELECT p FROM Producto p
            JOIN FETCH p.categoria JOIN FETCH p.unidad
            WHERE p.uuidProducto = :uuid AND p.negocio.idNegocio = :idNegocio
            """)
    Optional<Producto> buscarPorUuid(@Param("uuid") UUID uuid, @Param("idNegocio") Long idNegocio);

    // ¿Existe otro producto con el mismo nombre + marca + modelo (sin distinguir mayúsculas)?
    @Query("""
            SELECT COUNT(p) > 0 FROM Producto p
            WHERE p.negocio.idNegocio = :idNegocio
              AND lower(p.nombre) = lower(:nombre)
              AND lower(coalesce(p.marca, '')) = lower(:marca)
              AND lower(coalesce(p.modelo, '')) = lower(:modelo)
              AND p.idProducto <> :excluirId
            """)
    boolean existeIdentidad(@Param("idNegocio") Long idNegocio, @Param("nombre") String nombre,
                            @Param("marca") String marca, @Param("modelo") String modelo,
                            @Param("excluirId") Long excluirId);

    // Atributos activos por su identificador público
    @Query("SELECT a FROM Atributo a WHERE a.uuidAtributo IN :uuids AND a.estatus = 'activo'")
    List<Atributo> atributosActivos(@Param("uuids") Collection<UUID> uuids);

    // Valores activos que el negocio puede usar (del sistema o propios) entre los uuids recibidos
    @Query("""
            SELECT v FROM AtributoValor v JOIN FETCH v.atributo
            WHERE v.uuidValor IN :uuids AND v.estatus = 'activo'
              AND (v.negocio IS NULL OR v.negocio.idNegocio = :idNegocio)
            """)
    List<AtributoValor> valoresUsables(@Param("uuids") Collection<UUID> uuids, @Param("idNegocio") Long idNegocio);

    // Ids de los atributos del producto, en su orden
    @Query(value = "SELECT id_atributo FROM producto_atributos WHERE id_producto = :idProducto ORDER BY orden, id_atributo",
            nativeQuery = true)
    List<Long> idsAtributosDeProducto(@Param("idProducto") Long idProducto);

    // Atributos del producto: filas [id_atributo, uuid_atributo, nombre], en su orden
    @Query(value = """
            SELECT a.id_atributo, a.uuid_atributo, a.nombre
            FROM producto_atributos pa JOIN atributos a ON a.id_atributo = pa.id_atributo
            WHERE pa.id_producto = :idProducto
            ORDER BY pa.orden, a.id_atributo
            """, nativeQuery = true)
    List<Object[]> atributosDeProducto(@Param("idProducto") Long idProducto);

    // Asocia un atributo al producto
    @Modifying
    @Query(value = """
            INSERT INTO producto_atributos (id_producto, id_atributo, id_negocio, orden)
            VALUES (:idProducto, :idAtributo, :idNegocio, :orden)
            """, nativeQuery = true)
    void asociarAtributo(@Param("idProducto") Long idProducto, @Param("idAtributo") Long idAtributo,
                         @Param("idNegocio") Long idNegocio, @Param("orden") int orden);

    // Resumen de existencias por producto (solo variantes activas):
    // filas [id_producto, total_variantes, stock_total, precio_min, precio_max, sin_existencia, stock_bajo]
    @Query(value = """
            SELECT id_producto, COUNT(*), COALESCE(SUM(stock_actual), 0), MIN(precio_publico), MAX(precio_publico),
                   COUNT(*) FILTER (WHERE stock_actual = 0),
                   COUNT(*) FILTER (WHERE stock_actual > 0 AND stock_minimo > 0 AND stock_actual <= stock_minimo)
            FROM variantes
            WHERE id_negocio = :idNegocio AND estatus = 'activo'
            GROUP BY id_producto
            """, nativeQuery = true)
    List<Object[]> resumenVariantes(@Param("idNegocio") Long idNegocio);

    // ¿Alguna variante del producto tiene movimientos de inventario?
    @Query(value = """
            SELECT COUNT(*) > 0 FROM movimientos_inventario m
            JOIN variantes v ON v.id_variante = m.id_variante
            WHERE v.id_producto = :idProducto
            """, nativeQuery = true)
    boolean tieneMovimientos(@Param("idProducto") Long idProducto);

    // ¿Alguna variante del producto aparece en una solicitud de compra?
    @Query(value = """
            SELECT COUNT(*) > 0 FROM solicitud_compra_detalles d
            JOIN variantes v ON v.id_variante = d.id_variante
            WHERE v.id_producto = :idProducto
            """, nativeQuery = true)
    boolean tieneSolicitudes(@Param("idProducto") Long idProducto);

    // Borrado completo de un producto SIN movimientos: historiales, variantes y el producto (en ese orden)
    @Modifying
    @Query(value = "DELETE FROM variante_precios_historial WHERE id_variante IN (SELECT id_variante FROM variantes WHERE id_producto = :id)",
            nativeQuery = true)
    void borrarHistorialPrecios(@Param("id") Long idProducto);

    @Modifying
    @Query(value = "DELETE FROM variante_costos_historial WHERE id_variante IN (SELECT id_variante FROM variantes WHERE id_producto = :id)",
            nativeQuery = true)
    void borrarHistorialCostos(@Param("id") Long idProducto);

    @Modifying
    @Query(value = "DELETE FROM variantes WHERE id_producto = :id", nativeQuery = true)
    void borrarVariantes(@Param("id") Long idProducto);
}
