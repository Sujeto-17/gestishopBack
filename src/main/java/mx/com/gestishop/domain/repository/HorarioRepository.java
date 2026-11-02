package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de horarios. "Visibles" para un negocio = los globales + los propios de ese negocio
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    // Horarios globales y propios del negocio (activos y cancelados), con sus días ya cargados
    @Query("""
            SELECT DISTINCT h FROM Horario h
            LEFT JOIN FETCH h.dias
            WHERE h.negocio IS NULL OR h.negocio.idNegocio = :idNegocio
            ORDER BY h.nombre ASC
            """)
    List<Horario> listarVisibles(@Param("idNegocio") Long idNegocio);

    // Un horario por su identificador público, solo si es global o del negocio (nunca de otro negocio)
    @Query("""
            SELECT h FROM Horario h
            LEFT JOIN FETCH h.dias
            WHERE h.uuidHorario = :uuidHorario
              AND (h.negocio IS NULL OR h.negocio.idNegocio = :idNegocio)
            """)
    Optional<Horario> buscarVisiblePorUuid(@Param("uuidHorario") UUID uuidHorario,
                                           @Param("idNegocio") Long idNegocio);

    /**
     * ¿Ya hay un horario (global o del negocio) con ese nombre, sin distinguir mayúsculas?
     * excluirId evita compararse consigo mismo al editar; al crear se pasa -1.
     */
    @Query("""
            SELECT COUNT(h) > 0 FROM Horario h
            WHERE lower(h.nombre) = lower(:nombre)
              AND (h.negocio IS NULL OR h.negocio.idNegocio = :idNegocio)
              AND h.idHorario <> :excluirId
            """)
    boolean existeNombre(@Param("nombre") String nombre,
                         @Param("idNegocio") Long idNegocio,
                         @Param("excluirId") Long excluirId);
}
