package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Atributo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos del catálogo global de atributos
public interface AtributoRepository extends JpaRepository<Atributo, Long> {

    // Todos los atributos, o solo los activos, en su orden de presentación
    @Query("""
            SELECT a FROM Atributo a
            WHERE (:soloActivos = false OR a.estatus = 'activo')
            ORDER BY a.orden ASC, a.nombre ASC
            """)
    List<Atributo> listar(@Param("soloActivos") boolean soloActivos);

    Optional<Atributo> findByUuidAtributo(UUID uuidAtributo);

    // ¿Existe otro atributo con ese nombre (sin distinguir mayúsculas)? excluirId evita compararse consigo mismo
    @Query("""
            SELECT COUNT(a) > 0 FROM Atributo a
            WHERE lower(a.nombre) = lower(:nombre) AND a.idAtributo <> :excluirId
            """)
    boolean existeNombre(@Param("nombre") String nombre, @Param("excluirId") Long excluirId);

    // ¿Algún producto usa el atributo?
    @Query(value = "SELECT COUNT(*) > 0 FROM producto_atributos WHERE id_atributo = :idAtributo", nativeQuery = true)
    boolean estaEnUso(@Param("idAtributo") Long idAtributo);
}
