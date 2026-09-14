package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {
}
