package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.AdminNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Acceso a datos de la tabla admin_negocio (relación N admins ↔ 1 negocio).
public interface AdminNegocioRepository extends JpaRepository<AdminNegocio, Long> {

    /**
     * Obtiene el primer negocio asociado a un usuario admin.
     * Se usa en el login: hoy un admin administra un solo negocio en la práctica,
     * pero la relación en BD ya soporta varios (dueño + esposa + familiar en el
     * MISMO negocio, o el mismo usuario en VARIOS negocios a futuro).
     */
    @Query("""
            SELECT an FROM AdminNegocio an
            WHERE an.usuario.idUsuario = :idUsuario
            """)
    List<AdminNegocio> buscarPorUsuario(@Param("idUsuario") Long idUsuario);

    // Variante que devuelve solo el primer registro (para negocios con un solo admin por sesión)
    @Query("""
            SELECT an FROM AdminNegocio an
            WHERE an.usuario.idUsuario = :idUsuario
            ORDER BY an.createdAt ASC
            LIMIT 1
            """)
    Optional<AdminNegocio> buscarPrimerNegocioDeUsuario(@Param("idUsuario") Long idUsuario);

    // Lista todos los administradores de un negocio específico.
    @Query("""
            SELECT an FROM AdminNegocio an
            WHERE an.negocio.idNegocio = :idNegocio
            """)
    List<AdminNegocio> buscarPorNegocio(@Param("idNegocio") Long idNegocio);

    /**
     * Verifica si un usuario ya es admin de un negocio específico,
     * para evitar duplicados antes de insertar (aunque el índice único de BD
     * también lo protege, esto da un mensaje de negocio más claro).
     */
    @Query("""
            SELECT CASE WHEN COUNT(an) > 0 THEN true ELSE false END
            FROM AdminNegocio an
            WHERE an.usuario.idUsuario = :idUsuario
              AND an.negocio.idNegocio = :idNegocio
            """)
    boolean existeRelacion(@Param("idUsuario") Long idUsuario, @Param("idNegocio") Long idNegocio);
}