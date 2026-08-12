package mx.com.gestishop.core.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoredFileDTO {
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long size;
    private String path;
    private String relativePath;
}
