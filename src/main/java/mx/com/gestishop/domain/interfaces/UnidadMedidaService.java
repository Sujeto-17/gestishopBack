package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.response.UnidadMedidaResponseDTO;

import java.util.List;

// Consulta del catálogo general de unidades de medida
public interface UnidadMedidaService {

    // Unidades activas en el orden del catálogo
    List<UnidadMedidaResponseDTO> listar();
}
