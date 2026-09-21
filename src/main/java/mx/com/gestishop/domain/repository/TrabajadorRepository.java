package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    // Busca un trabajador por el identificador interno de su registro laboral, con Usuario ya cargado
    @Query("""
        SELECT t FROM Trabajador t
        JOIN FETCH t.usuario
        WHERE t.idTrabajador = :idTrabajador
        """)
    Optional<Trabajador> buscarPorId(@Param("idTrabajador") Long idTrabajador);

    /**
     * Lista todos los trabajadores activos de un negocio, con su Usuario ya cargado.
     * Se usa en el panel del admin para ver su plantilla de personal.
     */
    @Query("""
        SELECT t FROM Trabajador t
        JOIN FETCH t.usuario u
        WHERE t.negocio.idNegocio = :idNegocio
          AND u.deletedAt IS NULL
        ORDER BY u.nombre ASC
        """)
    List<Trabajador> listarPorNegocio(@Param("idNegocio") Long idNegocio);
}
