package mx.com.gestishop.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// Valor que toma una variante en un atributo (Color = Rojo)
@Getter
@Builder
public class VarianteValorResponseDTO {

    private UUID uuidAtributo;
    private String atributo;
    private UUID uuidValor;
    private String valor;
}
