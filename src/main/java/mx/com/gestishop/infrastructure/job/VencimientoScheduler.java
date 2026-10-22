package mx.com.gestishop.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.com.gestishop.domain.interfaces.SuscripcionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el proceso de vencimientos de suscripciones:
 *  - todas las noches a las 00:10 (hora de México)
 *  - una vez al arrancar la aplicación (por si el servidor estuvo apagado a esa hora)
 * Pasa a 'inactivo' los negocios cuyo periodo terminó y marca como 'vencida' las renovaciones sin pagar.
 * El corte real del acceso no depende de este job: lo hace el login comparando la fecha.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientoScheduler {

    private final SuscripcionService suscripcionService;

    // Corrida programada: segundo minuto hora día mes día-de-semana
    @Scheduled(cron = "0 10 0 * * *", zone = "America/Mexico_City")
    public void procesoNocturno() {
        ejecutar("nocturno");
    }

    // Corrida al iniciar: se pone al día cualquier vencimiento ocurrido mientras la app estuvo apagada
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar("arranque");
    }

    // Nunca debe tumbar la aplicación: si falla, solo se registra el error
    private void ejecutar(String origen) {
        try {
            suscripcionService.procesarVencimientos();
        } catch (Exception ex) {
            log.error("Falló el proceso de vencimientos ({}): {}", origen, ex.getMessage(), ex);
        }
    }
}
