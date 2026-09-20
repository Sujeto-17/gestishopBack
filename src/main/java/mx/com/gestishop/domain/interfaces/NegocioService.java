package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.NegocioRequestDTO;
import mx.com.gestishop.application.dto.response.NegocioResponseDTO;

import java.util.List;
import java.util.UUID;

// Contrato de gestión de negocios registrados en la plataforma
public interface NegocioService {

    NegocioResponseDTO crear(NegocioRequestDTO dto);

    NegocioResponseDTO actualizar(UUID uuidNegocio, NegocioRequestDTO dto);

    // Suspende o reactiva un negocio (alterna entre 'activo' e 'inactivo')
    NegocioResponseDTO cambiarEstatus(UUID uuidNegocio);

    NegocioResponseDTO obtenerPorUuid(UUID uuidNegocio);

    List<NegocioResponseDTO> listarActivos();
}
