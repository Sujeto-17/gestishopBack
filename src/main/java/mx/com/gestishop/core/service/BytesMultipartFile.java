package mx.com.gestishop.core.service;

import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Envuelve bytes en memoria como MultipartFile, para reusar FileStorageUtil.save()
 *  con contenido que ya no viene directo del request (ej. una imagen comprimida). */
public class BytesMultipartFile implements MultipartFile {

    private final byte[] contenido;
    private final String nombreOriginal;
    private final String contentType;

    public BytesMultipartFile(byte[] contenido, String nombreOriginal, String contentType) {
        this.contenido = contenido;
        this.nombreOriginal = nombreOriginal;
        this.contentType = contentType;
    }

    @Override
    public @NonNull String getName() { return "foto"; }

    @Override
    public String getOriginalFilename() { return nombreOriginal; }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public boolean isEmpty() { return contenido.length == 0; }

    @Override
    public long getSize() { return contenido.length; }

    @Override
    public byte @NonNull [] getBytes() { return contenido; }

    @Override
    public @NonNull InputStream getInputStream() { return new ByteArrayInputStream(contenido); }

    @Override
    public void transferTo(@NonNull java.io.File dest) throws IOException, IllegalStateException {
        java.nio.file.Files.write(dest.toPath(), contenido);
    }
}
