package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Acceso a datos de la tabla suscripciones (historial de periodos y pagos por negocio)
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    /**
     * Lista el historial completo de un negocio, del periodo más reciente al más antiguo.
     * Trae el plan ya cargado (JOIN FETCH) para evitar una consulta extra por cada fila
     * al construir la respuesta.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        JOIN FETCH s.plan
        WHERE s.negocio.idNegocio = :idNegocio
        ORDER BY s.fechaInicio DESC
        """)
    List<Suscripcion> listarPorNegocio(@Param("idNegocio") Long idNegocio);

    /**
     * Los periodos que DAN ACCESO a un negocio: la prueba y los pagados (los cancelados no cuentan).
     * Con ellos se calcula hasta cuándo tiene acceso (ver AccesoNegocioCalculator).
     */
    @Query("""
        SELECT s FROM Suscripcion s
        WHERE s.negocio.idNegocio = :idNegocio
          AND s.estatus IN ('prueba', 'pagada')
        """)
    List<Suscripcion> listarPeriodosConAcceso(@Param("idNegocio") Long idNegocio);
}
