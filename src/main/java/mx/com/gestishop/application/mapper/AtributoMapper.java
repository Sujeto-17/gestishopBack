package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.response.AtributoResponseDTO;
import mx.com.gestishop.application.dto.response.AtributoValorResponseDTO;
import mx.com.gestishop.domain.model.Atributo;
import mx.com.gestishop.domain.model.AtributoValor;

import java.util.List;

// Convierte entre entidades de atributos/valores y sus DTOs
public class AtributoMapper {

    private AtributoMapper() {
    }

    // Respuesta de un atributo con los valores ya filtrados por el service
    public static AtributoResponseDTO toResponse(Atributo a, List<AtributoValorResponseDTO> valores) {
        return AtributoResponseDTO.builder()
                .uuidAtributo(a.getUuidAtributo())
                .nombre(a.getNombre())
                .orden(a.getOrden())
                .estatus(a.getEstatus())
                .valores(valores)
                .build();
    }

    public static AtributoValorResponseDTO toResponse(AtributoValor v) {
        return AtributoValorResponseDTO.builder()
                .uuidValor(v.getUuidValor())
                .valor(v.getValor())
                .orden(v.getOrden())
                .estatus(v.getEstatus())
                .propio(v.esPropio())
                .build();
    }
}

