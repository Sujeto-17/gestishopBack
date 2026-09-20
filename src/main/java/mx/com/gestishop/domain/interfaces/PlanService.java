package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.PlanRequestDTO;
import mx.com.gestishop.application.dto.response.PlanResponseDTO;

import java.util.List;
import java.util.UUID;

// Contrato de gestión de planes de suscripción
public interface PlanService {

    PlanResponseDTO crear(PlanRequestDTO dto);

    PlanResponseDTO actualizar(UUID uuidPlan, PlanRequestDTO dto);

    PlanResponseDTO cambiarEstatus(UUID uuidPlan);

    PlanResponseDTO obtenerPorUuid(UUID uuidPlan);

    List<PlanResponseDTO> listarTodos();

    // Lista planes activos filtrados por tipo de sistema, usado en el alta de un negocio
    List<PlanResponseDTO> listarActivosPorSistema(String sistemaType);
}
