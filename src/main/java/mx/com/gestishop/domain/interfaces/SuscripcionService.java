package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.CancelarPeriodoRequestDTO;
import mx.com.gestishop.application.dto.request.SuscripcionRequestDTO;
import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.model.Suscripcion;

import java.util.List;

/**
 * Contrato de la gestión de suscripciones (periodos y pagos de cada negocio).
 * <p>
 * Un periodo solo puede estar en uno de tres estados:
 *  - prueba:    los primeros 15 días de un negocio nuevo (precio 0)
 *  - pagada:    30 días ya cobrados
 *  - cancelada: un periodo pagado que se registró por error (no da acceso)
 */
public interface SuscripcionService {

    // Crea el periodo de prueba de un negocio recién registrado (se llama desde NegocioService.crear)
    Suscripcion crearPeriodoInicial(Negocio negocio, Plan plan);

    /**
     * Acción principal del superadmin cuando un cliente paga: crea el periodo de 30 días ya pagado,
     * deja el negocio 'activo' y mueve su vencimiento al fin de ese periodo.
     * Si todavía le quedaban días, el nuevo periodo empieza donde termina el actual.
     */
    SuscripcionResponseDTO registrarPago(Long idNegocio, SuscripcionRequestDTO dto);

    /**
     * Cancela un periodo pagado (por ejemplo, registrado en el negocio equivocado) y recalcula el acceso
     * del negocio con los periodos que quedan. Un negocio suspendido a mano no se reactiva por cancelar.
     */
    SuscripcionResponseDTO cancelarPeriodo(Long idSuscripcion, CancelarPeriodoRequestDTO dto);

    // Historial de un negocio, del periodo más reciente al más antiguo
    List<SuscripcionResponseDTO> listarPorNegocio(Long idNegocio);

    /**
     * Proceso que corre cada noche (y al arrancar): pasa a 'inactivo' los negocios cuyo periodo terminó.
     * Devuelve cuántos negocios desactivó.
     */
    int procesarVencimientos();
}
