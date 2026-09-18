package mx.com.gestishop.infrastructure.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class SecureTokenUtil {

    // Usa una fuente de entropía apta para criptografía, indispensable para tokens de sesión.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureTokenUtil() {
    }

    /**
     * Genera un token opaco de 64 bytes (512 bits) codificado en Base64 URL-safe.
     * 512 bits es más que suficiente para hacer inviable un ataque de fuerza bruta.
     */
    public static String generar() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
