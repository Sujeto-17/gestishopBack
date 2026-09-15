package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Acceso a datos de la tabla cat_modulos.
 * El superadmin administra este catálogo desde su propio panel CRUD.
 */
public interface CatModuloRepository extends JpaRepository<CatModulo, Long> {

    /**
     * Lista todos los módulos activos, ordenados para armar el sidebar/selector.
     * Se usa en el panel de Planes (selección de módulos) y en el CRUD del superadmin.
     */
    @Query("""
        SELECT m FROM CatModulo m
        WHERE m.estatus = 'activo'
        ORDER BY m.orden ASC
        """)
    List<CatModulo> listarActivos();
}
