package mx.com.gestishop.core.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredImageDTO {

    private byte[] bytes;
    private String contentType;
}
