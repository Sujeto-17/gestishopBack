package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la bitácora de movimientos de inventario (solo se insertan; nunca se editan)
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
}

