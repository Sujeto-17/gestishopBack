package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de variantes; siempre acotado al negocio
public interface VarianteRepository extends JpaRepository<Variante, Long> {

    // Variantes de un producto en el orden en que se crearon
    @Query("SELECT v FROM Variante v WHERE v.producto.idProducto = :idProducto ORDER BY v.idVariante ASC")
    List<Variante> listarPorProducto(@Param("idProducto") Long idProducto);

    // Una variante de un producto del negocio
    @Query("""
            SELECT v FROM Variante v
            WHERE v.uuidVariante = :uuid AND v.producto.idProducto = :idProducto AND v.idNegocio = :idNegocio
            """)
    Optional<Variante> buscarPorUuid(@Param("uuid") UUID uuid, @Param("idProducto") Long idProducto,
                                     @Param("idNegocio") Long idNegocio);

    // Una variante del negocio con su producto y unidad de medida cargados (para registrar movimientos de inventario)
    @Query("""
            SELECT v FROM Variante v
            JOIN FETCH v.producto p
            JOIN FETCH p.unidad
            WHERE v.uuidVariante = :uuid AND v.idNegocio = :idNegocio
            """)
    Optional<Variante> buscarPorUuidEnNegocio(@Param("uuid") UUID uuid, @Param("idNegocio") Long idNegocio);

    // ¿Otro SKU igual en el negocio (sin distinguir mayúsculas)?
    @Query("""
            SELECT COUNT(v) > 0 FROM Variante v
            WHERE v.idNegocio = :idNegocio AND lower(v.sku) = lower(:sku) AND v.idVariante <> :excluirId
            """)
    boolean existeSku(@Param("idNegocio") Long idNegocio, @Param("sku") String sku, @Param("excluirId") Long excluirId);

    // ¿Otro código de barras igual en el negocio?
    @Query("""
            SELECT COUNT(v) > 0 FROM Variante v
            WHERE v.idNegocio = :idNegocio AND v.codigoBarras = :codigo AND v.idVariante <> :excluirId
            """)
    boolean existeCodigoBarras(@Param("idNegocio") Long idNegocio, @Param("codigo") String codigo,
                               @Param("excluirId") Long excluirId);

    // ¿La variante tiene movimientos?
    @Query(value = "SELECT COUNT(*) > 0 FROM movimientos_inventario WHERE id_variante = :idVariante", nativeQuery = true)
    boolean tieneMovimientos(@Param("idVariante") Long idVariante);

    // Valores de cada variante del producto: filas [id_variante, uuid_atributo, atributo, uuid_valor, valor],
    // en el orden de los atributos del producto
    @Query(value = """
            SELECT vv.id_variante, a.uuid_atributo, a.nombre, av.uuid_valor, av.valor
            FROM variante_valores vv
            JOIN producto_atributos pa ON pa.id_producto = vv.id_producto AND pa.id_atributo = vv.id_atributo
            JOIN atributos a ON a.id_atributo = vv.id_atributo
            JOIN atributo_valores av ON av.id_valor = vv.id_valor
            WHERE vv.id_producto = :idProducto
            ORDER BY vv.id_variante, pa.orden, a.id_atributo
            """, nativeQuery = true)
    List<Object[]> valoresDeProducto(@Param("idProducto") Long idProducto);

    // Asigna el valor de un atributo a la variante
    @Modifying
    @Query(value = """
            INSERT INTO variante_valores (id_variante, id_producto, id_atributo, id_valor)
            VALUES (:idVariante, :idProducto, :idAtributo, :idValor)
            """, nativeQuery = true)
    void asignarValor(@Param("idVariante") Long idVariante, @Param("idProducto") Long idProducto,
                      @Param("idAtributo") Long idAtributo, @Param("idValor") Long idValor);
}
