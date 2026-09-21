package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.SuscripcionRequestDTO;
import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.model.Suscripcion;

import java.util.List;

// Contrato de gestión del historial de suscripciones (pagos) de los negocios.
public interface SuscripcionService {

    /**
     * Crea el primer periodo de un negocio recién dado de alta (estatus 'pendiente').
     * Se llama desde NegocioServiceImpl al momento de crear un negocio, dentro de
     * la misma transacción, para que ambas filas se creen juntas o ninguna lo haga.
     */
    Suscripcion crearPeriodoInicial(Negocio negocio, Plan plan);

    // Marca un periodo como pagado, registrando fecha y metodo de pago
    SuscripcionResponseDTO confirmarPago(Long idSuscripcion, SuscripcionRequestDTO dto);

    /**
     * Genera el siguiente periodo de un negocio (30 días desde hoy o desde el
     * vencimiento anterior, lo que sea más tarde, para no perder días ya pagados).
     */
    SuscripcionResponseDTO renovar(Long idNegocio);

    List<SuscripcionResponseDTO> listarPorNegocio(Long idNegocio);

    // Lista negocios con periodos vencidos que nunca se pagaron (morosos)
    List<SuscripcionResponseDTO> listarVencidasPendientes();
}
