package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Acceso a datos de la tabla suscripciones (historial de pagos por negocio)
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
     * Obtiene el periodo VIGENTE actual de un negocio (el más reciente por fecha de inicio).
     * Se usa para saber si el negocio está al corriente o para confirmar su pago.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        JOIN FETCH s.plan
        WHERE s.negocio.idNegocio = :idNegocio
        ORDER BY s.fechaInicio DESC
        LIMIT 1
        """)
    Optional<Suscripcion> buscarPeriodoVigente(@Param("idNegocio") Long idNegocio);

    /**
     * Lista los periodos vencidos (fecha_vencimiento ya pasó) que siguen marcados
     * como 'pendiente' (nunca se pagaron). Útil para un reporte de negocios morosos.
     */
    @Query("""
        SELECT s FROM Suscripcion s
        JOIN FETCH s.negocio
        JOIN FETCH s.plan
        WHERE s.estatus = 'pendiente'
          AND s.fechaVencimiento < :hoy
        ORDER BY s.fechaVencimiento ASC
        """)
    List<Suscripcion> listarVencidasPendientes(@Param("hoy") LocalDate hoy);
}
