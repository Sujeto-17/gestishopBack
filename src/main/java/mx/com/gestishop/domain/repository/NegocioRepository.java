package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {
}
