package mx.com.gestishop.infrastructure.utils;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fecha "de hoy" para las reglas de negocio (vencimientos, periodos de prueba, pagos).
 * <p>
 * Usa SIEMPRE la zona de México y no la del servidor. Si el servidor corre en UTC,
 * LocalDate.now() ya marca el día siguiente desde las 6 pm (hora de México) y un negocio
 * vencería un día antes (o su prueba duraría un día de más).
 */
public final class FechaNegocioUtil {

    // Misma zona que spring.jackson.time-zone y hibernate.jdbc.time_zone del application.properties
    public static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private FechaNegocioUtil() {
    }

    // Hoy, según el calendario de México
    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
