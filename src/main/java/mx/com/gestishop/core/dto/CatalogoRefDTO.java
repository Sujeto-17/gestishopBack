package mx.com.gestishop.core.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogoRefDTO {

    private Integer id;
    private String nombre;
}
