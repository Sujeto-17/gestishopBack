package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a la bitácora de movimientos de inventario (solo se insertan; nunca se editan)
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    // Une a la variante su etiqueta ("Rojo / M") armada con los valores de sus atributos, en el orden del producto
    String ETIQUETA_VARIANTE = """
            LEFT JOIN LATERAL (
                SELECT string_agg(av.valor, ' / ' ORDER BY pa.orden, a.id_atributo) AS etiqueta
                FROM variante_valores vv
                JOIN producto_atributos pa ON pa.id_producto = vv.id_producto AND pa.id_atributo = vv.id_atributo
                JOIN atributos a ON a.id_atributo = vv.id_atributo
                JOIN atributo_valores av ON av.id_valor = vv.id_valor
                WHERE vv.id_variante = v.id_variante
            ) et ON true
            """;

    // Columnas de la consulta de movimientos, en este orden:
    // 0 uuid_movimiento, 1 created_at, 2 tipo, 3 motivo, 4 cantidad, 5 stock_anterior, 6 stock_resultante,
    // 7 uuid_variante, 8 producto, 9 etiqueta de la variante, 10 unidad, 11 usuario, 12 proveedor, 13 costo_unitario, 14 notas
    String SELECT_MOVIMIENTOS = """
            SELECT m.uuid_movimiento, m.created_at, m.tipo, m.motivo, m.cantidad, m.stock_anterior, m.stock_resultante,
                   v.uuid_variante, p.nombre, COALESCE(et.etiqueta, ''), un.clave, us.nombre, pr.nombre, m.costo_unitario, m.notas
            FROM movimientos_inventario m
            JOIN variantes v ON v.id_variante = m.id_variante
            JOIN productos p ON p.id_producto = v.id_producto
            JOIN cat_unidades_medida un ON un.id_unidad = p.id_unidad
            JOIN usuarios us ON us.id_usuario = m.id_usuario
            LEFT JOIN proveedores pr ON pr.id_proveedor = m.id_proveedor
            """ + ETIQUETA_VARIANTE;

    /**
     * Movimientos del negocio del más reciente al más antiguo. Los filtros en null no se aplican.
     * desde (incluida) y hasta (excluida) acotan la fecha del movimiento. soloPendientes: resurtidos sin costo. soloOperativos: solo compras, mermas y consumos (lo que ve el gerente).
     */
    @Query(value = SELECT_MOVIMIENTOS + """
            WHERE m.id_negocio = :idNegocio
              AND (CAST(:idVariante AS BIGINT) IS NULL OR m.id_variante = :idVariante)
              AND (CAST(:uuidMovimiento AS TEXT) IS NULL OR CAST(m.uuid_movimiento AS TEXT) = :uuidMovimiento)
              AND (CAST(:tipo AS TEXT) IS NULL OR m.tipo = :tipo)
              AND (CAST(:motivo AS TEXT) IS NULL OR m.motivo = :motivo)
              AND (CAST(:desde AS TIMESTAMPTZ) IS NULL OR m.created_at >= :desde)
              AND (CAST(:hasta AS TIMESTAMPTZ) IS NULL OR m.created_at < :hasta)
              AND (:soloPendientes = false OR (m.tipo = 'entrada' AND m.motivo = 'compra' AND m.costo_unitario IS NULL))
              AND (:soloOperativos = false OR m.motivo IN ('compra', 'merma', 'consumo'))
            ORDER BY m.created_at DESC, m.id_movimiento DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> consultar(@Param("idNegocio") Long idNegocio, @Param("idVariante") Long idVariante,
                             @Param("uuidMovimiento") String uuidMovimiento, @Param("tipo") String tipo,
                             @Param("motivo") String motivo, @Param("desde") OffsetDateTime desde,
                             @Param("hasta") OffsetDateTime hasta, @Param("soloPendientes") boolean soloPendientes,
                             @Param("soloOperativos") boolean soloOperativos, @Param("limite") int limite);

    // Variantes activas (de productos activos) para elegir al registrar movimientos: filas
    // [uuid_variante, producto, etiqueta, existencia, unidad]
    @Query(value = """
            SELECT v.uuid_variante, p.nombre, COALESCE(et.etiqueta, ''), v.stock_actual, un.clave
            FROM variantes v
            JOIN productos p ON p.id_producto = v.id_producto
            JOIN cat_unidades_medida un ON un.id_unidad = p.id_unidad
            """ + ETIQUETA_VARIANTE + """
            WHERE v.id_negocio = :idNegocio AND v.estatus = 'activo' AND p.estatus = 'activo'
            ORDER BY p.nombre, v.id_variante
            """, nativeQuery = true)
    List<Object[]> variantesActivas(@Param("idNegocio") Long idNegocio);

    // Proveedores activos del negocio, solo identificador y nombre: filas [uuid_proveedor, nombre]
    @Query(value = """
            SELECT uuid_proveedor, nombre FROM proveedores
            WHERE id_negocio = :idNegocio AND estatus = 'activo'
            ORDER BY nombre
            """, nativeQuery = true)
    List<Object[]> proveedoresActivos(@Param("idNegocio") Long idNegocio);

    // Un movimiento del negocio con su variante y proveedor cargados
    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.variante
            LEFT JOIN FETCH m.proveedor
            WHERE m.uuidMovimiento = :uuid AND m.idNegocio = :idNegocio
            """)
    Optional<MovimientoInventario> buscarPorUuid(@Param("uuid") UUID uuid, @Param("idNegocio") Long idNegocio);

    // Completa el costo de un movimiento que lo tenía vacío (la BD no permite cambiarlo si ya tenía valor).
    // Devuelve cuántas filas cambió: 0 si ya tenía costo.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE movimientos_inventario SET costo_unitario = :costo WHERE id_movimiento = :id AND costo_unitario IS NULL",
            nativeQuery = true)
    int asignarCosto(@Param("id") Long idMovimiento, @Param("costo") BigDecimal costo);
}
