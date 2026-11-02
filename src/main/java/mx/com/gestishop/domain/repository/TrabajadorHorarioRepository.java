package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.TrabajadorHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Acceso a datos del historial de horarios de los trabajadores
public interface TrabajadorHorarioRepository extends JpaRepository<TrabajadorHorario, Long> {

    // ¿Algún trabajador usó alguna vez este horario? (si sí, el horario queda bloqueado: solo se cancela)
    boolean existsByHorario_IdHorario(Long idHorario);

    // ¿Algún trabajador lo tiene vigente hoy o programado a futuro? (si sí, no se puede cancelar)
    @Query("""
            SELECT COUNT(th) > 0 FROM TrabajadorHorario th
            WHERE th.horario.idHorario = :idHorario
              AND (th.vigenteHasta IS NULL OR th.vigenteHasta >= :hoy)
            """)
    boolean existeVigente(@Param("idHorario") Long idHorario, @Param("hoy") LocalDate hoy);

    // De una lista de horarios, los ids de los que alguna vez se asignaron
    @Query("SELECT DISTINCT th.horario.idHorario FROM TrabajadorHorario th WHERE th.horario.idHorario IN :ids")
    List<Long> idsUsados(@Param("ids") Collection<Long> ids);

    // Filas [idHorario, cantidad] de trabajadores que tienen cada horario vigente o programado
    @Query("""
            SELECT th.horario.idHorario, COUNT(th) FROM TrabajadorHorario th
            WHERE th.horario.idHorario IN :ids
              AND (th.vigenteHasta IS NULL OR th.vigenteHasta >= :hoy)
            GROUP BY th.horario.idHorario
            """)
    List<Object[]> contarVigentes(@Param("ids") Collection<Long> ids, @Param("hoy") LocalDate hoy);

    // El registro "abierto" (sin fecha final) de un trabajador: su horario actual
    @Query("""
            SELECT th FROM TrabajadorHorario th
            JOIN FETCH th.horario
            WHERE th.trabajador.idTrabajador = :idTrabajador
              AND th.vigenteHasta IS NULL
            """)
    Optional<TrabajadorHorario> buscarAbierto(@Param("idTrabajador") Long idTrabajador);

    // Última fecha final registrada en el historial (null si no hay ninguna cerrada)
    @Query("SELECT MAX(th.vigenteHasta) FROM TrabajadorHorario th WHERE th.trabajador.idTrabajador = :idTrabajador")
    LocalDate ultimaFechaFin(@Param("idTrabajador") Long idTrabajador);

    // Horarios que rigen hoy para TODOS los trabajadores del negocio, en una sola consulta (para el listado)
    @Query("""
            SELECT DISTINCT th FROM TrabajadorHorario th
            JOIN FETCH th.horario h
            LEFT JOIN FETCH h.dias
            JOIN FETCH th.trabajador t
            JOIN FETCH t.usuario
            WHERE t.negocio.idNegocio = :idNegocio
              AND th.vigenteDesde <= :hoy
              AND (th.vigenteHasta IS NULL OR th.vigenteHasta >= :hoy)
            """)
    List<TrabajadorHorario> vigentesDelNegocio(@Param("idNegocio") Long idNegocio, @Param("hoy") LocalDate hoy);

    // Historial completo de un trabajador, del más reciente al más antiguo, con horario y días cargados
    @Query("""
            SELECT DISTINCT th FROM TrabajadorHorario th
            JOIN FETCH th.horario h
            LEFT JOIN FETCH h.dias
            WHERE th.trabajador.idTrabajador = :idTrabajador
            ORDER BY th.vigenteDesde DESC
            """)
    List<TrabajadorHorario> historial(@Param("idTrabajador") Long idTrabajador);
}
