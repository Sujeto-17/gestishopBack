package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.PlanRequestDTO;
import mx.com.gestishop.application.dto.response.PlanResponseDTO;
import mx.com.gestishop.application.mapper.PlanMapper;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.PlanService;
import mx.com.gestishop.domain.model.CatModulo;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.repository.CatModuloRepository;
import mx.com.gestishop.domain.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación de la gestión de planes. La relación con módulos (N:M) se
 * resuelve aquí: se reciben IDs en el DTO, se cargan las entidades CatModulo
 * correspondientes, y se asignan al Set de la entidad Plan — Hibernate se
 * encarga de sincronizar la tabla intermedia plan_modulos automáticamente.
 */
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final CatModuloRepository catModuloRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponseDTO crear(PlanRequestDTO dto) {
        Plan entidad = PlanMapper.toEntity(dto);
        entidad.setModulos(cargarModulos(dto.getIdModulos()));

        Plan guardado = RepositoryExecutor.execute(() -> planRepository.save(entidad), "Plan", "crear");
        return PlanMapper.toResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponseDTO actualizar(UUID uuidPlan, PlanRequestDTO dto) {
        Plan entidad = RepositoryExecutor.execute(
                () -> planRepository.buscarPorUuidConModulos(uuidPlan)
                        .orElseThrow(() -> ErrorFactory.notFound("Plan", uuidPlan)),
                "Plan", "actualizar"
        );

        PlanMapper.actualizarEntidad(entidad, dto);
        // Reemplaza por completo el set de módulos: se limpia y se vuelve a poblar
        // con los IDs recibidos, así se reflejan tanto altas como bajas de módulos.
        entidad.getModulos().clear();
        entidad.getModulos().addAll(cargarModulos(dto.getIdModulos()));

        Plan actualizado = RepositoryExecutor.execute(() -> planRepository.save(entidad), "Plan", "actualizar");
        return PlanMapper.toResponse(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponseDTO cambiarEstatus(UUID uuidPlan) {
        Plan entidad = RepositoryExecutor.execute(
                () -> planRepository.buscarPorUuidConModulos(uuidPlan)
                        .orElseThrow(() -> ErrorFactory.notFound("Plan", uuidPlan)),
                "Plan", "cambiarEstatus"
        );

        entidad.setEstatus("activo".equals(entidad.getEstatus()) ? "inactivo" : "activo");

        Plan actualizado = RepositoryExecutor.execute(() -> planRepository.save(entidad), "Plan", "cambiarEstatus");
        return PlanMapper.toResponse(actualizado);
    }

    @Override
    public PlanResponseDTO obtenerPorUuid(UUID uuidPlan) {
        Plan entidad = RepositoryExecutor.execute(
                () -> planRepository.buscarPorUuidConModulos(uuidPlan)
                        .orElseThrow(() -> ErrorFactory.notFound("Plan", uuidPlan)),
                "Plan", "consultar"
        );
        return PlanMapper.toResponse(entidad);
    }

    @Override
    public List<PlanResponseDTO> listarTodos() {
        return RepositoryExecutor.execute(planRepository::listarTodosConModulos, "Plan", "listarTodos")
                .stream().map(PlanMapper::toResponse).toList();
    }

    @Override
    public List<PlanResponseDTO> listarActivosPorSistema(String sistemaType) {
        return RepositoryExecutor.execute(
                        () -> planRepository.buscarActivosPorSistema(sistemaType), "Plan", "listarActivosPorSistema")
                .stream().map(PlanMapper::toResponse).toList();
    }

    // Carga las entidades CatModulo correspondientes a la lista de IDs recibida en el DTO
    private Set<CatModulo> cargarModulos(List<Long> idModulos) {
        return new HashSet<>(RepositoryExecutor.execute(
                () -> catModuloRepository.buscarPorIds(idModulos), "CatModulo", "cargarParaPlan"));
    }
}
