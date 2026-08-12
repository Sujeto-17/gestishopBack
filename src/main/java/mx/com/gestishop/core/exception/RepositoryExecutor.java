package mx.com.gestishop.core.exception;

import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.resolver.ConstraintMessageResolver;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.*;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RepositoryExecutor {

    public static <T> T execute(Supplier<T> action, String entidad, String operacion) {
        try {
            return action.get();
        } catch (Exception ex) {
            throw translate(ex, entidad, operacion);
        }
    }

    public static void executeVoid(Runnable action, String entidad, String operacion) {
        try {
            action.run();
        } catch (Exception ex) {
            throw translate(ex, entidad, operacion);
        }
    }

    public static <T> List<T> executeList(Supplier<List<T>> action, String entidad, String operacion) {
        List<T> resultado = execute(action, entidad, operacion);

        if (resultado == null || resultado.isEmpty()) {
            throw new ApiResponseException(
                    ApiCodeResponse.NOT_FOUND,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "detalle", "No se encontraron registros."
                    )
            );
        }

        return resultado;
    }

    // -------------------------------------------------------------------------
    // Traductor de excepciones
    // -------------------------------------------------------------------------
    private static ApiResponseException translate(
            Exception ex,
            String entidad,
            String operacion
    ) {
        /*
         * Primero revisar si proviene de PostgreSQL
         */
        PSQLException psql = findPSQLException(ex);

        if (psql != null) {
            return translatePostgreSQL(psql, entidad, operacion);
        }

        /*
         * Errores técnicos de Spring - Conexión
         */
        if (ex instanceof CannotGetJdbcConnectionException) {
            return new ApiResponseException(
                    ApiCodeResponse.DATABASE_UNAVAILABLE,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion
                    ),
                    ex
            );
        }

        /*
         * Inicio de transacción
         */
        if (ex instanceof CannotCreateTransactionException) {
            return new ApiResponseException(
                    ApiCodeResponse.DATABASE_UNAVAILABLE,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "detalle", "No fue posible iniciar la transacción."
                    ),
                    ex
            );
        }

        /*
         * Timeout
         */
        if (ex instanceof QueryTimeoutException) {
            return new ApiResponseException(
                    ApiCodeResponse.TIMEOUT,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion
                    ),
                    ex
            );
        }

        /*
         * Conflictos de concurrencia
         */
        if (ex instanceof CannotAcquireLockException ||
                ex instanceof DeadlockLoserDataAccessException ||
                ex instanceof OptimisticLockingFailureException) {
            return new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "detalle", "Conflicto de concurrencia."
                    ),
                    ex
            );
        }

        /*
         * Violación de integridad
         */
        if (ex instanceof DataIntegrityViolationException) {
            return new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "constraint", ConstraintMessageResolver.getConstraintName(ex),
                            "detalle", ConstraintMessageResolver.resolve(ex)
//                            "detalle", getMostSpecificMessage(ex)
                    ),
                    ex
            );
        }

        /*
         * Uso incorrecto de la API
         */
        if (ex instanceof InvalidDataAccessApiUsageException || ex instanceof IllegalArgumentException) {
            return new ApiResponseException(
                    ApiCodeResponse.BAD_REQUEST,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "detalle", ex.getMessage()
                    ),
                    ex
            );
        }

        /*
         * Recursos BD
         */
        if (ex instanceof DataAccessResourceFailureException) {
            return new ApiResponseException(
                    ApiCodeResponse.DATABASE_UNAVAILABLE,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion
                    ),
                    ex
            );
        }

        /*
         * Error general DataAccess
         */
        if (ex instanceof DataAccessException) {
            return new ApiResponseException(
                    ApiCodeResponse.DATABASE_ERROR,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "detalle", getMostSpecificMessage(ex)
                    ),
                    ex
            );
        }

        /*
         * Cualquier otra excepción - Error inesperado
         */
        return new ApiResponseException(
                ApiCodeResponse.INTERNAL_ERROR,
                Map.of(
                        "entidad", entidad,
                        "operacion", operacion,
                        "detalle", ex.getMessage()
                ),
                ex
        );
    }

    // -------------------------------------------------------------------------
    // Traducción de errores PostgreSQL
    // -------------------------------------------------------------------------
    private static ApiResponseException translatePostgreSQL(
            PSQLException ex,
            String entidad,
            String operacion
    ) {
        ServerErrorMessage server = ex.getServerErrorMessage();

        String sqlState = server != null ? server.getSQLState() : ex.getSQLState();
        String message = server != null ? server.getMessage() : ex.getMessage();
        String detail = server != null ? server.getDetail() : null;
        String hint = server != null ? server.getHint() : null;

        // UNIQUE
        // FK
        // CHECK
        // NOT NULL
        // etc.
        if (sqlState != null && sqlState.startsWith("23")) {
            return new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "constraint", server.getConstraint(),
                            "detalle", ConstraintMessageResolver.resolve(server.getConstraint())
                    ),
                    ex
            );
        }

        /*
         * Todos los códigos Pxxxx se consideran reglas de negocio provenientes de PostgreSQL.
         * Errores lanzados desde funciones PostgreSQL mediante RAISE EXCEPTION.
         */
        if (sqlState != null && sqlState.startsWith("P")) {
            return new ApiResponseException(
                    ApiCodeResponse.CONFLICT,
                    Map.of(
                            "entidad", entidad,
                            "operacion", operacion,
                            "sqlState", sqlState,
                            "mensaje", message,
                            "detalle", detail,
                            "hint", hint
                    ),
                    ex
            );
        }

        /*
         * Cualquier otro SQLSTATE es considerado un error técnico.
         */
        return new ApiResponseException(
                ApiCodeResponse.DATABASE_ERROR,
                Map.of(
                        "entidad", entidad,
                        "operacion", operacion,
                        "sqlState", sqlState,
                        "mensaje", message,
                        "detalle", detail
                ),
                ex
        );
    }

    // -------------------------------------------------------------------------
    // Utilitarios
    // -------------------------------------------------------------------------
    private static PSQLException findPSQLException(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof PSQLException psql) {
                return psql;
            }

            current = current.getCause();
        }

        return null;
    }

    private static String getMostSpecificMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage();
    }
}
