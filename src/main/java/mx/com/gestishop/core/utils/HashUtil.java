package mx.com.gestishop.core.utils;

import mx.com.gestishop.core.enums.AlgorithmHash;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtil {

    private HashUtil() {
        super();
    }

    // ==================  HASH DESDE STRING  ==================
    private static byte[] hash(String input, AlgorithmHash algorithm) {
        return hash(input.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    public static String hashHex(String input, AlgorithmHash algorithm) {
        return toHex(hash(input, algorithm));
    }

    public static String hashBase64(String input, AlgorithmHash algorithm) {
        return toBase64(hash(input, algorithm));
    }

    // ==================  HASH DESDE BYTE[] ===================
    private static byte[] hash(byte[] data, AlgorithmHash algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.value());
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algoritmo no soportado: " + algorithm, e);
        }
    }

    public static String hashHex(byte[] data, AlgorithmHash algorithm) {
        return toHex(hash(data, algorithm));
    }

    public static String hashBase64(byte[] data, AlgorithmHash algorithm) {
        return toBase64(hash(data, algorithm));
    }

    // =================  HASH DESDE ARCHIVO  ==================
    private static byte[] hash(Path path, AlgorithmHash algorithm) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return hash(is, algorithm);
        }
    }

    public static String hashHex(Path path, AlgorithmHash algorithm) throws IOException {
        return toHex(hash(path, algorithm));
    }

    public static String hashBase64(Path path, AlgorithmHash algorithm) throws IOException {
        return toBase64(hash(path, algorithm));
    }

    private static byte[] hash(InputStream inputStream, AlgorithmHash algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.value());

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return digest.digest();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algoritmo no soportado: " + algorithm, e);
        }
    }

    // ======================  ENCODING  =======================
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

}
