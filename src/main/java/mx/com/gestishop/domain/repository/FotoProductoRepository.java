package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Archivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Fotos de los productos: filas de la tabla archivos con tipo "producto" y categoría "foto_producto"
public interface FotoProductoRepository extends JpaRepository<Archivo, Long> {

    // Fotos vigentes del producto: primero la principal y luego las demás por su orden
    @Query("""
            SELECT a FROM Archivo a
            WHERE a.tipoEntidad.clave = 'producto' AND a.categoria = 'foto_producto'
              AND a.idEntidad = :idProducto AND a.deletedAt IS NULL
            ORDER BY a.esPrincipal DESC, a.orden ASC, a.idArchivo ASC
            """)
    List<Archivo> listar(@Param("idProducto") Long idProducto);

    // Una foto vigente del producto por su uuid (valida que pertenezca a ese producto)
    @Query("""
            SELECT a FROM Archivo a
            WHERE a.tipoEntidad.clave = 'producto' AND a.categoria = 'foto_producto'
              AND a.idEntidad = :idProducto AND a.uuidArchivo = :uuidArchivo AND a.deletedAt IS NULL
            """)
    Optional<Archivo> buscarPorUuid(@Param("idProducto") Long idProducto, @Param("uuidArchivo") UUID uuidArchivo);

    // Cuántas fotos vigentes tiene el producto
    @Query("""
            SELECT COUNT(a) FROM Archivo a
            WHERE a.tipoEntidad.clave = 'producto' AND a.categoria = 'foto_producto'
              AND a.idEntidad = :idProducto AND a.deletedAt IS NULL
            """)
    long contar(@Param("idProducto") Long idProducto);

    // Bloquea la fila del producto hasta terminar la transacción, para que dos subidas simultáneas no pasen del límite
    @Query(value = "SELECT p.id_producto FROM productos p WHERE p.id_producto = :idProducto FOR UPDATE", nativeQuery = true)
    List<Long> bloquearProducto(@Param("idProducto") Long idProducto);

    // Foto principal de cada producto del negocio, para el listado: filas [id_producto, ruta_relativa, nombre_archivo]
    @Query(value = """
            SELECT a.id_entidad, a.ruta_relativa, a.nombre_archivo
            FROM archivos a
            JOIN cat_tipos_entidad_archivo t ON t.id_tipo_entidad = a.id_tipo_entidad
            JOIN productos p ON p.id_producto = a.id_entidad
            WHERE t.clave = 'producto' AND a.categoria = 'foto_producto'
              AND a.es_principal = true AND a.deleted_at IS NULL
              AND p.id_negocio = :idNegocio
            """, nativeQuery = true)
    List<Object[]> principalesDelNegocio(@Param("idNegocio") Long idNegocio);
}
