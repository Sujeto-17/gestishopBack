package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatTipoEntidadArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CatTipoEntidadArchivoRepository extends JpaRepository<CatTipoEntidadArchivo, Long> {

    @Query("SELECT t FROM CatTipoEntidadArchivo t WHERE t.clave = :clave")
    Optional<CatTipoEntidadArchivo> buscarPorClave(@Param("clave") String clave);
}
