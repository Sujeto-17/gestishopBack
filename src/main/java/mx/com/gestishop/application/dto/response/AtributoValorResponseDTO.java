package mx.com.gestishop.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// Valor de un atributo; propio = true si lo creó el negocio (puede editarlo o quitarlo)
@Getter
@Builder
public class AtributoValorResponseDTO {
    private UUID uuidValor;
    private String valor;
    private Short orden;
    private String estatus;
    private boolean propio;
}

