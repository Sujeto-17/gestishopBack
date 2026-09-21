package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.TrabajadorRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorResponseDTO;

import java.util.List;

// Contrato de gestión de trabajadores (identidad de acceso + datos laborales)
public interface TrabajadorService {

    // Da de alta un trabajador: crea su Usuario y su registro laboral en una sola transacción
    TrabajadorResponseDTO crear(TrabajadorRequestDTO dto);

    TrabajadorResponseDTO actualizar(Long idTrabajador, TrabajadorRequestDTO dto);

    // Activa/desactiva la cuenta de acceso del trabajador (afecta la tabla usuarios, no trabajadores)
    TrabajadorResponseDTO cambiarEstatus(Long idTrabajador);

    TrabajadorResponseDTO obtenerPorId(Long idTrabajador);

    List<TrabajadorResponseDTO> listarPorNegocio(Long idNegocio);
}
