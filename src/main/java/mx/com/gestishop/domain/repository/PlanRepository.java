package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
}
