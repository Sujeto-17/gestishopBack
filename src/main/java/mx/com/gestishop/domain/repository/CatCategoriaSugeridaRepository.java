package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatCategoriaSugerida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatCategoriaSugeridaRepository extends JpaRepository<CatCategoriaSugerida, Long> {

    // Plantillas de un giro (familias y subfamilias) con su padre cargado
    @Query("""
            SELECT s FROM CatCategoriaSugerida s
            LEFT JOIN FETCH s.padre
            WHERE s.giro.idGiro = :idGiro
            ORDER BY s.orden ASC, s.nombre ASC
            """)
    List<CatCategoriaSugerida> listarPorGiro(@Param("idGiro") Long idGiro);
}
