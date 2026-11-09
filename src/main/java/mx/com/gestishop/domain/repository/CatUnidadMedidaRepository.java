package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatUnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatUnidadMedidaRepository extends JpaRepository<CatUnidadMedida, Long> {

    // Unidades activas en el orden del catálogo
    List<CatUnidadMedida> findByEstatusOrderByOrdenAsc(String estatus);
}
