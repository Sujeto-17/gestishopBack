package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Acceso a datos de la tabla trabajadores (extiende usuarios con datos laborales)
public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {

    /**
     * Obtiene los datos laborales (puesto, nivel de acceso, negocio) de un usuario.
     * Se usa en el login para resolver el contexto de un usuario tipo 'trabajador'.
     */
    @Query("""
            SELECT t FROM Trabajador t
            WHERE t.usuario.idUsuario = :idUsuario
            """)
    Optional<Trabajador> buscarPorUsuario(@Param("idUsuario") Long idUsuario);
}
