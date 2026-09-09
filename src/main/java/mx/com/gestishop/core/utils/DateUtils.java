package mx.com.gestishop.core.utils;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class DateUtils {

    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMATO_FECHA_LARGA = DateTimeFormatter
            .ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"));

    private DateUtils() {
    }

    // Formatea una fecha en formato corto (dd/MM/yyyy)
    public static String formatearCorta(TemporalAccessor fecha) {
        return fecha != null ? FORMATO_FECHA.format(fecha) : "";
    }

    // Formatea una fecha en formato largo capitalizado ("Jueves, 20 de agosto de 2026")
    public static String formatearLargaCapitalizada(TemporalAccessor fecha) {
        if (fecha == null) return "";
        String fechaFormateada = FORMATO_FECHA_LARGA.format(fecha);
        return fechaFormateada.substring(0, 1).toUpperCase() + fechaFormateada.substring(1);
    }

    // Genera la cadena de ubicación y fecha larga ("Villahermosa, Tabasco, Jueves...")
    public static String formatearFechaLugar(TemporalAccessor fecha) {
        return "Villahermosa, Tabasco, " + formatearLargaCapitalizada(fecha);
    }
}
