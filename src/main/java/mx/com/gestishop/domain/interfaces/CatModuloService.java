package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.CatModuloRequestDTO;
import mx.com.gestishop.application.dto.response.ModuloResponseDTO;

import java.util.List;

// Contrato de gestión del catálogo de módulos del sistema
public interface CatModuloService {

    ModuloResponseDTO crear(CatModuloRequestDTO dto);

    ModuloResponseDTO actualizar(Long idModulo, CatModuloRequestDTO dto);

    ModuloResponseDTO cambiarEstatus(Long idModulo);

    List<ModuloResponseDTO> listarTodos();

    List<ModuloResponseDTO> listarActivos();
}
