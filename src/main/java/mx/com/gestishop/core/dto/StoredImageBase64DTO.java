package mx.com.gestishop.core.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredImageBase64DTO {

    private String base64;
    private String contentType;
}
