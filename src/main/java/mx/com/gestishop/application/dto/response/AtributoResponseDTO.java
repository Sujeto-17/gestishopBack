package mx.com.gestishop.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

// Atributo con sus valores visibles para quien consulta
@Getter
@Builder
public class AtributoResponseDTO {

    private UUID uuidAtributo;
    private String nombre;
    private Short orden;
    private String estatus;
    private List<AtributoValorResponseDTO> valores;
}

