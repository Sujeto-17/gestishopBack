package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Acceso a datos de la tabla cat_modulos. El superadmin administra este catálogo
public interface CatModuloRepository extends JpaRepository<CatModulo, Long> {

    // Lista los módulos activos, ordenados. Se usa al armar selects y sidebar
    @Query("SELECT m FROM CatModulo m WHERE m.estatus = 'activo' ORDER BY m.orden ASC")
    List<CatModulo> listarActivos();

    // Lista TODOS los módulos, para el CRUD del superadmin
    @Query("SELECT m FROM CatModulo m ORDER BY m.orden ASC")
    List<CatModulo> listarTodos();

    // Busca varios módulos por sus IDs, usado al asignar módulos a un plan
    @Query("SELECT m FROM CatModulo m WHERE m.idModulo IN :ids")
    List<CatModulo> buscarPorIds(@Param("ids") List<Long> ids);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM CatModulo m WHERE m.nombre = :nombre")
    boolean existeNombre(@Param("nombre") String nombre);
}
