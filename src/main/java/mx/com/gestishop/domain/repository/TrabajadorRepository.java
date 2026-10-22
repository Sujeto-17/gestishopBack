package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de la tabla trabajadores (extiende usuarios con datos laborales)
public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {

    /**
     * Obtiene los datos laborales de un usuario, junto con su Usuario ya cargado
     * (JOIN FETCH evita una segunda consulta al mapear la respuesta).
     * Se usa en el login para resolver el contexto de un usuario tipo 'trabajador'.
     */
    @Query("""
        SELECT t FROM Trabajador t
        JOIN FETCH t.usuario
        WHERE t.usuario.idUsuario = :idUsuario
        """)
    Optional<Trabajador> buscarPorUsuario(@Param("idUsuario") Long idUsuario);

    // Busca un trabajador por el identificador público (UUID) de su usuario asociado
    @Query("""
    SELECT t FROM Trabajador t
    JOIN FETCH t.usuario u
    LEFT JOIN FETCH t.genero
    LEFT JOIN FETCH t.estadoCivil
    LEFT JOIN FETCH t.municipio
    WHERE u.uuidUsuario = :uuidUsuario
    """)
    Optional<Trabajador> buscarPorUuidUsuario(@Param("uuidUsuario") UUID uuidUsuario);

    @Query("""
    SELECT t FROM Trabajador t
    JOIN FETCH t.usuario u
    LEFT JOIN FETCH t.genero
    LEFT JOIN FETCH t.estadoCivil
    LEFT JOIN FETCH t.municipio
    WHERE u.uuidUsuario = :uuidUsuario
      AND t.negocio.idNegocio = :idNegocio
    """)
    Optional<Trabajador> buscarPorUuidUsuarioYNegocio(@Param("uuidUsuario") UUID uuidUsuario,
                                                      @Param("idNegocio") Long idNegocio);

    // Lista todos los trabajadores activos de un negocio, con su Usuario ya cargado
    @Query("""
    SELECT t FROM Trabajador t
    JOIN FETCH t.usuario u
    LEFT JOIN FETCH t.genero
    LEFT JOIN FETCH t.estadoCivil
    LEFT JOIN FETCH t.municipio
    WHERE t.negocio.idNegocio = :idNegocio
      AND u.deletedAt IS NULL
    ORDER BY u.nombre ASC
    """)
    List<Trabajador> listarPorNegocio(@Param("idNegocio") Long idNegocio);

    // Cuántos trabajadores ACTIVOS tiene un negocio (para el límite del plan)
    @Query("""
            SELECT COUNT(t) FROM Trabajador t
            WHERE t.negocio.idNegocio = :idNegocio
              AND t.usuario.estatus = 'activo'
              AND t.usuario.deletedAt IS NULL
            """)
    long contarActivosPorNegocio(@Param("idNegocio") Long idNegocio);

    // Lo mismo para TODOS los negocios en una sola consulta: filas [idNegocio, cantidad] (para el listado del superadmin)
    @Query("""
            SELECT t.negocio.idNegocio, COUNT(t) FROM Trabajador t
            WHERE t.usuario.estatus = 'activo'
              AND t.usuario.deletedAt IS NULL
            GROUP BY t.negocio.idNegocio
            """)
    List<Object[]> contarActivosAgrupados();
}
