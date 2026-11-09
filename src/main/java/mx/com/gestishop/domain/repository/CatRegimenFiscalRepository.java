package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatRegimenFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatRegimenFiscalRepository extends JpaRepository<CatRegimenFiscal, String> {

    // Regímenes activos ordenados por clave
    List<CatRegimenFiscal> findByEstatusOrderByClaveAsc(String estatus);
}
