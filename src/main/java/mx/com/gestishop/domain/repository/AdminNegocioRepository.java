package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.AdminNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de la tabla admin_negocio (relación N:M entre admins y negocios)
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

    /**
     * Trae el primer negocio asignado a un usuario admin, identificado por su UUID
     * público (en vez de id interno), con Usuario y Negocio ya cargados (JOIN FETCH)
     * para evitar consultas extra al construir la respuesta de un solo admin.
     */
    @Query("""
        SELECT an FROM AdminNegocio an
        JOIN FETCH an.usuario u
        JOIN FETCH an.negocio n
        WHERE u.uuidUsuario = :uuidUsuario
        ORDER BY an.createdAt ASC
        LIMIT 1
        """)
    Optional<AdminNegocio> buscarPorUuidUsuario(@Param("uuidUsuario") UUID uuidUsuario);

    // Lista TODOS los admins activos del sistema, cada uno con su negocio ya cargado
    @Query("""
        SELECT an FROM AdminNegocio an
        JOIN FETCH an.usuario u
        JOIN FETCH an.negocio n
        WHERE u.deletedAt IS NULL
        ORDER BY u.nombre ASC
        """)
    List<AdminNegocio> listarTodos();

    /**
     * Todos los negocios de un admin, con el negocio y su plan ya cargados (JOIN FETCH), sin los negocios
     * borrados. Es la base del selector de negocio del login y de las reglas del "último admin".
     */
    @Query("""
            SELECT an FROM AdminNegocio an
            JOIN FETCH an.negocio n
            JOIN FETCH n.plan
            WHERE an.usuario.idUsuario = :idUsuario
              AND n.deletedAt IS NULL
            ORDER BY an.createdAt ASC
            """)
    List<AdminNegocio> buscarPorUsuarioConNegocio(@Param("idUsuario") Long idUsuario);

    /**
     * Cuenta los OTROS administradores activos de un negocio (sin contar al indicado).
     * Si da 0, el admin indicado es el único activo: no se debe desactivar ni desvincular.
     */
    @Query("""
            SELECT COUNT(an) FROM AdminNegocio an
            WHERE an.negocio.idNegocio = :idNegocio
              AND an.usuario.idUsuario <> :idUsuario
              AND an.usuario.estatus = 'activo'
              AND an.usuario.deletedAt IS NULL
            """)
    long contarOtrosAdminsActivos(@Param("idNegocio") Long idNegocio, @Param("idUsuario") Long idUsuario);

    // La relación de UN admin con UN negocio concreto (para desvincularlo), con usuario y negocio cargados
    @Query("""
            SELECT an FROM AdminNegocio an
            JOIN FETCH an.usuario u
            JOIN FETCH an.negocio n
            WHERE u.uuidUsuario = :uuidUsuario
              AND n.idNegocio = :idNegocio
            """)
    Optional<AdminNegocio> buscarRelacion(@Param("uuidUsuario") UUID uuidUsuario, @Param("idNegocio") Long idNegocio);

    // Cuántos administradores ACTIVOS tiene un negocio (para el límite del plan y el aviso "sin administrador")
    @Query("""
            SELECT COUNT(an) FROM AdminNegocio an
            WHERE an.negocio.idNegocio = :idNegocio
              AND an.usuario.estatus = 'activo'
              AND an.usuario.deletedAt IS NULL
            """)
    long contarAdminsActivos(@Param("idNegocio") Long idNegocio);

    // Lo mismo para TODOS los negocios en una sola consulta: filas [idNegocio, cantidad]
    @Query("""
            SELECT an.negocio.idNegocio, COUNT(an) FROM AdminNegocio an
            WHERE an.usuario.estatus = 'activo'
              AND an.usuario.deletedAt IS NULL
            GROUP BY an.negocio.idNegocio
            """)
    List<Object[]> contarAdminsActivosAgrupados();
}
