package mx.com.gestishop.core.service;

import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Valida y normaliza imágenes subidas por usuarios antes de tocar el disco.
 * <p>
 * Principio: el archivo original NUNCA se guarda. Se comprueba que realmente sea una imagen JPG o PNG
 * (por su contenido, no por nombre ni por el tipo que declara el cliente), se decodifica y se genera
 * un JPEG nuevo. Con ello cualquier script, archivo incrustado o metadato (EXIF, GPS) del original se descarta.
 */
@Component
public class ImagenSeguraProcesador {

    // Peso máximo aceptado del archivo recibido
    private static final long BYTES_MAXIMOS = 5L * 1024 * 1024;
    // Máximo de píxeles permitidos (evita imágenes pequeñas en bytes pero enormes al decodificar)
    private static final long PIXELES_MAXIMOS = 25_000_000L;
    private static final int LADO_MAXIMO_PX = 1200;
    private static final int LADO_MINIATURA_PX = 300;
    private static final float CALIDAD_JPEG = 0.82f;
    private static final float CALIDAD_MINIATURA = 0.75f;

    // Resultado del procesamiento: bytes del JPEG final y, si se pidió, de su miniatura
    public record ImagenProcesada(byte[] principal, byte[] miniatura) { }

    /**
     * Valida el archivo recibido y devuelve el JPEG normalizado (y miniatura opcional).
     *
     * @param archivo         archivo enviado por el cliente
     * @param generarMiniatura true para producir además una versión pequeña para listados
     * @throws ApiResponseException si el archivo no es una imagen JPG/PNG válida o excede los límites
     */
    public ImagenProcesada procesar(MultipartFile archivo, boolean generarMiniatura) {
        byte[] bytes = leerBytes(archivo);
        Formato formato = detectarFormato(bytes);
        validarDimensiones(bytes, formato);

        byte[] principal = convertirAJpeg(bytes, formato, LADO_MAXIMO_PX, CALIDAD_JPEG);
        byte[] miniatura = generarMiniatura
                ? convertirAJpeg(principal, Formato.JPEG, LADO_MINIATURA_PX, CALIDAD_MINIATURA)
                : null;
        return new ImagenProcesada(principal, miniatura);
    }

    private enum Formato { JPEG, PNG }

    // Lee el archivo completo en memoria respetando el límite de peso y rechazando archivos vacíos.
    private byte[] leerBytes(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw rechazo("No se recibió ningún archivo.");
        }
        if (archivo.getSize() > BYTES_MAXIMOS) {
            throw rechazo("La imagen excede el máximo de 5 MB.");
        }
        try {
            byte[] bytes = archivo.getBytes();
            if (bytes.length > BYTES_MAXIMOS) {
                throw rechazo("La imagen excede el máximo de 5 MB.");
            }
            return bytes;
        } catch (IOException ex) {
            throw rechazo("No fue posible leer el archivo.");
        }
    }

    // Identifica el formato por los primeros bytes (firma real). Solo JPG y PNG; cualquier otra cosa se rechaza.
    private Formato detectarFormato(byte[] b) {
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Formato.JPEG;
        }
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Formato.PNG;
        }
        throw rechazo("Solo se permiten imágenes JPG o PNG.");
    }

    // Lee solo la cabecera para conocer el tamaño en píxeles y rechaza imágenes desproporcionadas
    // antes de decodificarlas. Además confirma que el lector de imágenes coincide con la firma detectada.
    private void validarDimensiones(byte[] bytes, Formato formato) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(in);
            if (!lectores.hasNext()) {
                throw rechazo("El archivo no es una imagen válida.");
            }
            ImageReader lector = lectores.next();
            try {
                String nombre = lector.getFormatName().toLowerCase();
                boolean coincide = formato == Formato.JPEG ? nombre.contains("jpeg") || nombre.contains("jpg")
                        : nombre.contains("png");
                if (!coincide) {
                    throw rechazo("El archivo no es una imagen válida.");
                }
                lector.setInput(in, true, true);
                long ancho = lector.getWidth(0);
                long alto = lector.getHeight(0);
                if (ancho <= 0 || alto <= 0 || ancho * alto > PIXELES_MAXIMOS) {
                    throw rechazo("Las dimensiones de la imagen son demasiado grandes.");
                }
            } finally {
                lector.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ApiResponseException api) throw api;
            throw rechazo("El archivo no es una imagen válida.");
        }
    }

    // Decodifica y vuelve a codificar como JPEG con el lado mayor limitado (sin agrandar imágenes pequeñas).
    // En PNG con transparencia el fondo se rellena de blanco, porque JPEG no admite alfa.
    private byte[] convertirAJpeg(byte[] bytes, Formato formato, int ladoMaximo, float calidad) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            // Si la imagen ya cabe en el límite se conserva su tamaño (escala 1.0); si no, se reduce
            boolean yaCabe = ladoMayor(bytes) <= ladoMaximo;

            Thumbnails.Builder<?> constructor;
            if (formato == Formato.PNG) {
                BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
                if (original == null) throw rechazo("El archivo no es una imagen válida.");
                constructor = Thumbnails.of(aplanarSobreBlanco(original));
            } else {
                // Desde el flujo, Thumbnailator respeta la orientación EXIF (fotos de celular) y no copia metadatos
                constructor = Thumbnails.of(new ByteArrayInputStream(bytes));
            }

            constructor = yaCabe ? constructor.scale(1.0) : constructor.size(ladoMaximo, ladoMaximo);
            constructor.outputFormat("jpg")
                    .outputQuality(calidad)
                    .toOutputStream(salida);
            return salida.toByteArray();
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof ApiResponseException api) throw api;
            throw rechazo("No fue posible procesar la imagen.");
        }
    }

    // Lee solo la cabecera y devuelve el lado más largo de la imagen en píxeles
    private int ladoMayor(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(in);
            if (!lectores.hasNext()) throw rechazo("El archivo no es una imagen válida.");
            ImageReader lector = lectores.next();
            try {
                lector.setInput(in, true, true);
                return Math.max(lector.getWidth(0), lector.getHeight(0));
            } finally {
                lector.dispose();
            }
        }
    }

    // Pinta la imagen sobre un lienzo RGB blanco para eliminar el canal alfa.
    private BufferedImage aplanarSobreBlanco(BufferedImage origen) {
        BufferedImage lienzo = new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lienzo.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, lienzo.getWidth(), lienzo.getHeight());
            g.drawImage(origen, 0, 0, null);
        } finally {
            g.dispose();
        }
        return lienzo;
    }

    private ApiResponseException rechazo(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
