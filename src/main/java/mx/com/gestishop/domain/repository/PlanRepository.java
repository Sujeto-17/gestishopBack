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

    // Busca un plan por su identificador público (UUID)
    @Query("SELECT p FROM Plan p WHERE p.uuidPlan = :uuidPlan")
    Optional<Plan> buscarPorUuid(@Param("uuidPlan") UUID uuidPlan);

    /**
     * Lista los planes activos de un tipo de sistema específico (servicio | tienda),
     * usado al mostrar el selector de planes en el alta de un negocio.
     */
    @Query("""
            SELECT p FROM Plan p
            WHERE p.sistemaType = :sistemaType
              AND p.estatus = 'activo'
            ORDER BY p.precio ASC
            """)
    List<Plan> buscarActivosPorSistema(@Param("sistemaType") String sistemaType);
}
