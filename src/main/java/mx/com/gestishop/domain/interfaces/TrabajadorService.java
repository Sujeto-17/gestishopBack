package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.TrabajadorRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorResponseDTO;

import java.util.List;
import java.util.UUID;

// Contrato de gestión de trabajadores (identidad de acceso + datos laborales)
public interface TrabajadorService {

    // Da de alta un trabajador: crea su Usuario y su registro laboral en una sola transacción
    TrabajadorResponseDTO crear(TrabajadorRequestDTO dto);

    TrabajadorResponseDTO actualizar(UUID uuidUsuario, TrabajadorRequestDTO dto);

    // Activa/desactiva la cuenta de acceso del trabajador (afecta la tabla usuarios, no trabajadores)
    TrabajadorResponseDTO cambiarEstatus(UUID uuidUsuario);

    TrabajadorResponseDTO obtenerPorUuid(UUID uuidUsuario);

    List<TrabajadorResponseDTO> listarPorNegocio(Long idNegocio);
}
