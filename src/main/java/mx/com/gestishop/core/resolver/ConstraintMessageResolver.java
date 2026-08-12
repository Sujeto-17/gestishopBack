package mx.com.gestishop.core.resolver;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.util.Map;

public class ConstraintMessageResolver {

    private ConstraintMessageResolver() {
    }

    private static final String DEFAULT_MESSAGE =
            "La operación no pudo realizarse debido a una restricción de integridad.";

    private static final Map<String, String> MENSAJES = Map.ofEntries(
            Map.entry("cat_ops_stk_uuid_uk", "Ya existe un registro con el UUID especificado.")
    );

    /**
     * Resuelve el mensaje a partir del nombre del constraint.
     */
    public static String resolve(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return DEFAULT_MESSAGE;
        }

        return MENSAJES.getOrDefault(constraintName, DEFAULT_MESSAGE);
    }

    /**
     * Resuelve directamente desde una excepción.
     */
    public static String resolve(Throwable throwable) {
        String constraint = getConstraintName(throwable);

        if (constraint == null) {
            return DEFAULT_MESSAGE;
        }

        return resolve(constraint);
    }

    /**
     * Obtiene el nombre del constraint recorriendo toda la cadena de causas.
     */
    public static String getConstraintName(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof PSQLException psql) {
                ServerErrorMessage server = psql.getServerErrorMessage();

                if (server != null && server.getConstraint() != null) {
                    return server.getConstraint();
                }
            }

            current = current.getCause();
        }

        return null;
    }
}
