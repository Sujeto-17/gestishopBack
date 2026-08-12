package mx.com.gestishop.core.enums;

import io.swagger.v3.oas.annotations.media.Schema;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Enum que representa los algoritmos de hash disponibles para el servicio de
 * incidencias.
 * Proporciona métodos para obtener el nombre del algoritmo y crear instancias
 * de MessageDigest.
 */
@Schema(name = "AlgorithmHash",
        description = "Algoritmos de hash disponibles para el servicio de incidencias")
public enum AlgorithmHash {

    MD5("MD5"),          // Solo compatibilidad legacy
    SHA1("SHA-1"),       // Solo compatibilidad legacy
    SHA256("SHA-256"),   // Estándar moderno
    SHA384("SHA-384"),   // Buena alternativa
    SHA512("SHA-512");   // Más robusto

    private final String value;

    AlgorithmHash(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * Crea una instancia de MessageDigest para este algoritmo.
     * @return MessageDigest configurado con el algoritmo especificado.
     * @throws IllegalStateException si el algoritmo no es soportado por la JVM.
     */
    public MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(this.value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo no soportado: " + this, e);
        }
    }

}
