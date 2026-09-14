package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.IntentoLogin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {
}
