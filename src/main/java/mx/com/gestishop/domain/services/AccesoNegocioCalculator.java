package mx.com.gestishop.domain.services;

import java.time.LocalDate;
import java.util.List;

/**
 * Regla ÚNICA que decide hasta cuándo tiene acceso un negocio y en qué estatus queda, a partir de
 * sus periodos de suscripción. Es una clase pura (sin base de datos ni Spring) para poder probarla
 * sola: ver AccesoNegocioCalculatorTest.
 * <p>
 * Solo cuentan los periodos que dan acceso: 'prueba' y 'pagada'. Los 'cancelada' se ignoran.
 * Se usa al registrar un pago y al cancelar un periodo, y siempre da el mismo resultado:
 * <pre>
 *   vencimiento = el fin del último periodo que da acceso
 *   estatus     = inactivo  si ya pasó el vencimiento
 *                 activo    si hay un periodo PAGADO en curso hoy
 *                 prueba    si solo queda la prueba en curso (o un pago que empieza más adelante)
 * </pre>
 */
public final class AccesoNegocioCalculator {

    // Un periodo reducido a lo que importa para el cálculo
    public record Periodo(String estatus, LocalDate inicio, LocalDate vencimiento) {
    }

    /** Lo que debe quedar guardado en el negocio. */
    public record Resultado(LocalDate vencimiento, String estatusNegocio) {
    }

    private AccesoNegocioCalculator() {
    }

    // ¿Este periodo da acceso? Solo la prueba y los pagados; los cancelados no cuentan
    private static boolean daAcceso(Periodo periodo) {
        return "prueba".equals(periodo.estatus()) || "pagada".equals(periodo.estatus());
    }

    /**
     * Fecha en que empieza el siguiente periodo que se pague.
     * Si al negocio todavía le quedan días, el nuevo periodo empieza donde termina el actual (no se
     * pierden días ya pagados); si ya venció o nunca tuvo, empieza hoy.
     */
    public static LocalDate inicioDelNuevoPeriodo(List<Periodo> periodos, LocalDate hoy) {
        LocalDate ultimoVencimiento = periodos.stream()
                .filter(AccesoNegocioCalculator::daAcceso)
                .map(Periodo::vencimiento)
                .max(LocalDate::compareTo)
                .orElse(null);

        return ultimoVencimiento != null && ultimoVencimiento.isAfter(hoy) ? ultimoVencimiento : hoy;
    }

    /** Calcula el vencimiento y el estatus que le corresponden al negocio según sus periodos. */
    public static Resultado calcular(List<Periodo> periodos, LocalDate hoy) {
        List<Periodo> conAcceso = periodos.stream().filter(AccesoNegocioCalculator::daAcceso).toList();

        // Sin ningún periodo que dé acceso, el negocio queda vencido desde ayer
        LocalDate vencimiento = conAcceso.stream()
                .map(Periodo::vencimiento)
                .max(LocalDate::compareTo)
                .orElse(hoy.minusDays(1));

        if (vencimiento.isBefore(hoy)) {
            return new Resultado(vencimiento, "inactivo");
        }

        boolean hayPagoEnCurso = conAcceso.stream().anyMatch(p ->
                "pagada".equals(p.estatus()) && !p.inicio().isAfter(hoy) && !p.vencimiento().isBefore(hoy));

        return new Resultado(vencimiento, hayPagoEnCurso ? "activo" : "prueba");
    }
}

