package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.CatGiroNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Acceso a datos de la tabla cat_giros_negocio. Catálogo administrado por el superadmin
public interface CatGiroNegocioRepository extends JpaRepository<CatGiroNegocio, Long> {

    // Lista todos los giros activos, ordenados para poblar selects del frontend
    @Query("""
        SELECT g FROM CatGiroNegocio g
        WHERE g.estatus = 'activo'
        ORDER BY g.orden ASC
        """)
    List<CatGiroNegocio> listarActivos();

    // Lista TODOS los giros (activos e inactivos), para el CRUD del superadmin
    @Query("SELECT g FROM CatGiroNegocio g ORDER BY g.orden ASC")
    List<CatGiroNegocio> listarTodos();

    // Verifica si ya existe un giro con ese nombre, para evitar duplicados antes del INSERT
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM CatGiroNegocio g WHERE g.nombre = :nombre")
    boolean existeNombre(@Param("nombre") String nombre);
}
