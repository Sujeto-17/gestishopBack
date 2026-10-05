package mx.com.gestishop.infrastructure.utils;

import java.security.SecureRandom;

/**
 * Genera contraseñas temporales legibles pero seguras, para el flujo de
 * "restablecer contraseña" que ejecuta el superadmin sobre un admin que
 * olvidó la suya. No confundir con SecureTokenUtil (ese genera tokens
 * opacos de sesión, no contraseñas pensadas para que un humano las lea/escriba).
 */
public class PasswordGeneratorUtil {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // sin I/O, se confunden visualmente
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789"; // sin 0/1, se confunden con O/l
    private static final String SIMBOLOS = "!@#$%&*";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + SIMBOLOS;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGeneratorUtil() {
    }

    // Genera una contraseña de 10 caracteres garantizando al menos un tipo de cada categoría
    public static String generar() {
        StringBuilder sb = new StringBuilder(10);
        sb.append(MAYUSCULAS.charAt(RANDOM.nextInt(MAYUSCULAS.length())));
        sb.append(MINUSCULAS.charAt(RANDOM.nextInt(MINUSCULAS.length())));
        sb.append(NUMEROS.charAt(RANDOM.nextInt(NUMEROS.length())));
        sb.append(SIMBOLOS.charAt(RANDOM.nextInt(SIMBOLOS.length())));

        for (int i = sb.length(); i < 10; i++) {
            sb.append(TODOS.charAt(RANDOM.nextInt(TODOS.length())));
        }

        // Mezcla los caracteres para que el orden no sea siempre [MAY][min][num][simb]...
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }

        return new String(chars);
    }
}