package mx.com.gestishop.core.utils;

import mx.com.gestishop.core.enums.TextAlternation;

import java.lang.reflect.Field;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class GeneralUtil {

    public GeneralUtil() {
        super();
    }

    public static String homotext(String texto) {
        return homotext(texto, TextAlternation.UPPERCASE);
    }

    public static String homotext(String texto, TextAlternation alternation) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }

        if (alternation == null) {
            alternation = TextAlternation.UPPERCASE;
        }

        String normalized = Normalizer.normalize(texto, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder();

        for (char c : normalized.toCharArray()) {
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                if (c == 'ñ') c = 'n';
                if (c == 'Ñ') c = 'N';
                if (c == ' ') c = '_';

                sb.append(c);
            }
        }

        return switch (alternation) {
            case UPPERCASE -> sb.toString().toUpperCase();
            case LOWERCASE -> sb.toString().toLowerCase();
            case NORMAL ->  sb.toString();
        };
    }

    public static void generarHomotext(Object objeto, String propiedad, String htPropiedad) {
        try {
            Field propField = objeto.getClass().getDeclaredField(propiedad);
            Field htField = objeto.getClass().getDeclaredField(htPropiedad);
            propField.setAccessible(true);
            htField.setAccessible(true);
            Object value = propField.get(objeto);
            if (value != null && !value.toString().isEmpty()) {
                htField.set(objeto, homotext(value.toString()));
            }
        }
        catch (final NoSuchFieldException | IllegalAccessException ex) {}
    }

    public static void generarHomotext(List<?> lista, String propiedad, String htPropiedad) {
        for (Object objeto : lista) {
            generarHomotext(objeto, propiedad, htPropiedad);
        }
    }

    public static void generarHomotextCadena(Object objeto, String cadena, String htPropiedad) {
        if (cadena != null && !cadena.trim().isEmpty()) {
            try {
                Field htField = objeto.getClass().getDeclaredField(htPropiedad);
                htField.setAccessible(true);
                htField.set(objeto, homotext(cadena));
            }
            catch (NoSuchFieldException | IllegalAccessException ex) {}
        }
    }

    public static UUID generarUUIDv4() {
        return UUID.randomUUID();
    }

    public static UUID generarUUIDv1() {
        long timestamp = System.currentTimeMillis();
        long randomValue = new Random().nextLong();
        return new UUID(timestamp, randomValue);
    }

    public static UUID toUuidV4(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            UUID uuid = UUID.fromString(value.trim());

            if (uuid.version() != 4) {
                throw new IllegalArgumentException("El UUID no es versión 4");
            }

            return uuid;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("UUID inválido: " + value, ex);
        }
    }

    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(capitalize(word));
            }
        }
        return result.toString();
    }

    public static String toCamelCaseExceptFirst(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.trim().split("\\s+");
        if (words.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; ++i) {
            result.append(capitalize(words[i]));
        }
        return result.toString();
    }

    public static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static String generateDatePath() {
        LocalDate now = LocalDate.now();
        return String.valueOf(now.getYear()) + "/" + String.format("%02d", now.getMonthValue());
    }

    /*
    public static String getUsername(String username) {
        String user = username;

        if (ValidatorUtils.isEmpty(user)) {
            user = AppConstants.USERNAME;
        }

        return user;
    }
     */
}
