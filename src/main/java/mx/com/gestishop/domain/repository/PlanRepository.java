package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de la tabla planes (catálogo de suscripción del superadmin)
public interface PlanRepository extends JpaRepository<Plan, Long> {

    // Busca un plan por su identificador público, junto con sus módulos ya cargados (evita N+1)
    @Query("""
        SELECT p FROM Plan p
        LEFT JOIN FETCH p.modulos
        WHERE p.uuidPlan = :uuidPlan
        """)
    Optional<Plan> buscarPorUuidConModulos(@Param("uuidPlan") UUID uuidPlan);

    // Lista los planes activos de un tipo de sistema, usado al mostrar el selector en el alta de negocio
    @Query("""
        SELECT DISTINCT p FROM Plan p
        LEFT JOIN FETCH p.modulos
        WHERE p.sistemaType = :sistemaType
          AND p.estatus = 'activo'
        ORDER BY p.precio ASC
        """)
    List<Plan> buscarActivosPorSistema(@Param("sistemaType") String sistemaType);

    // Lista TODOS los planes con sus módulos, para el CRUD del superadmin
    @Query("SELECT DISTINCT p FROM Plan p LEFT JOIN FETCH p.modulos ORDER BY p.sistemaType, p.precio ASC")
    List<Plan> listarTodosConModulos();
}
