package mx.com.gestishop.core.utils;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class ValidatorUtils {

    private ValidatorUtils() {
    }

    public static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String str) {
            return str.trim().isEmpty();
        }

        if (value instanceof UUID uuid) {
            return uuid.toString().trim().isEmpty();
        }

        if (value instanceof Collection<?> col) {
            return col.isEmpty();
        }

        if (value instanceof Optional<?> opt) {
            return opt.isEmpty();
        }

        return false;
    }
}
