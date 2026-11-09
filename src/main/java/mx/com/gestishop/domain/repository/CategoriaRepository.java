package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de categorías. Todas las consultas filtran por negocio: nunca se ve una categoría de otro negocio
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Todas las categorías del negocio (familias y subfamilias) con su padre ya cargado
    @Query("""
            SELECT c FROM Categoria c
            LEFT JOIN FETCH c.padre
            WHERE c.negocio.idNegocio = :idNegocio
            ORDER BY c.orden ASC, c.nombre ASC
            """)
    List<Categoria> listarPorNegocio(@Param("idNegocio") Long idNegocio);

    // Una categoría por su identificador público, solo si es del negocio
    @Query("""
            SELECT c FROM Categoria c
            LEFT JOIN FETCH c.padre
            WHERE c.uuidCategoria = :uuid AND c.negocio.idNegocio = :idNegocio
            """)
    Optional<Categoria> buscarPorUuid(@Param("uuid") UUID uuid, @Param("idNegocio") Long idNegocio);

    // ¿Existe una familia con ese nombre (sin distinguir mayúsculas)? excluirId evita compararse consigo misma al editar
    @Query("""
            SELECT COUNT(c) > 0 FROM Categoria c
            WHERE c.negocio.idNegocio = :idNegocio AND c.padre IS NULL
              AND lower(c.nombre) = lower(:nombre) AND c.idCategoria <> :excluirId
            """)
    boolean existeNombreRaiz(@Param("idNegocio") Long idNegocio, @Param("nombre") String nombre,
                             @Param("excluirId") Long excluirId);

    // ¿Existe una subfamilia con ese nombre dentro de la misma familia?
    @Query("""
            SELECT COUNT(c) > 0 FROM Categoria c
            WHERE c.negocio.idNegocio = :idNegocio AND c.padre.idCategoria = :idPadre
              AND lower(c.nombre) = lower(:nombre) AND c.idCategoria <> :excluirId
            """)
    boolean existeNombreHija(@Param("idNegocio") Long idNegocio, @Param("idPadre") Long idPadre,
                             @Param("nombre") String nombre, @Param("excluirId") Long excluirId);

    // Cantidad de subfamilias de una familia
    long countByPadre_IdCategoria(Long idPadre);

    // Subfamilias de una familia (para inactivarlas junto con ella)
    List<Categoria> findByPadre_IdCategoria(Long idPadre);

    // Cuántos productos tiene cada categoría del negocio: filas [idCategoria, cantidad]
    @Query(value = """
            SELECT id_categoria, COUNT(*) FROM productos
            WHERE id_negocio = :idNegocio
            GROUP BY id_categoria
            """, nativeQuery = true)
    List<Object[]> contarProductosPorCategoria(@Param("idNegocio") Long idNegocio);

    // ¿La categoría tiene al menos un producto (activo o no)?
    @Query(value = "SELECT COUNT(*) > 0 FROM productos WHERE id_categoria = :idCategoria", nativeQuery = true)
    boolean tieneProductos(@Param("idCategoria") Long idCategoria);
}
