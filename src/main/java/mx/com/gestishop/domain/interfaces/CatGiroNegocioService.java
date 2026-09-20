package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.CatGiroNegocioRequestDTO;
import mx.com.gestishop.application.dto.response.CatGiroNegocioResponseDTO;

import java.util.List;

// Contrato de gestión del catálogo de giros de negocio
public interface CatGiroNegocioService {

    CatGiroNegocioResponseDTO crear(CatGiroNegocioRequestDTO dto);

    CatGiroNegocioResponseDTO actualizar(Long idGiro, CatGiroNegocioRequestDTO dto);

    // Cambia el estatus entre activo/inactivo (no se borra físicamente el catálogo)
    CatGiroNegocioResponseDTO cambiarEstatus(Long idGiro);

    List<CatGiroNegocioResponseDTO> listarTodos();

    List<CatGiroNegocioResponseDTO> listarActivos();
}
