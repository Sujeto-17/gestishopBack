package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatTolerancia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Acceso a datos del catálogo de tolerancias (solo lectura desde la aplicación)
public interface CatToleranciaRepository extends JpaRepository<CatTolerancia, Long> {

    // Todas las tolerancias, de menor a mayor, para llenar el selector
    List<CatTolerancia> findAllByOrderByMinutosAsc();
}
