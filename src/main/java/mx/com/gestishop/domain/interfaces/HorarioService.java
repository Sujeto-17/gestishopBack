package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.HorarioRequestDTO;
import mx.com.gestishop.application.dto.response.HorarioResponseDTO;
import mx.com.gestishop.application.dto.response.ToleranciaResponseDTO;

import java.util.List;
import java.util.UUID;

// Gestión de horarios de un negocio (los globales se pueden consultar y usar, no modificar)
public interface HorarioService {

    // Horarios globales + propios del negocio; con soloActivos=true se omiten los cancelados (para los selectores)
    List<HorarioResponseDTO> listar(Long idNegocio, boolean soloActivos);

    HorarioResponseDTO obtener(UUID uuidHorario, Long idNegocio);

    HorarioResponseDTO crear(HorarioRequestDTO dto, Long idNegocio);

    // Solo horarios propios que nadie ha usado
    HorarioResponseDTO actualizar(UUID uuidHorario, HorarioRequestDTO dto, Long idNegocio);

    // Cancela (activo -> inactivo) o reactiva (inactivo -> activo); cancelar exige que ningún trabajador lo tenga vigente
    HorarioResponseDTO cambiarEstatus(UUID uuidHorario, Long idNegocio);

    // Solo horarios propios que nadie ha usado
    void eliminar(UUID uuidHorario, Long idNegocio);

    // Catálogo de tolerancias (0, 5... 30 minutos)
    List<ToleranciaResponseDTO> listarTolerancias();
}
