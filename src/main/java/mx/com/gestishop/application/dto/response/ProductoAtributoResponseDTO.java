package mx.com.gestishop.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// Atributo con el que se generan las variantes de un producto
@Getter
@Builder
public class ProductoAtributoResponseDTO {

    private UUID uuidAtributo;
    private String nombre;
}
