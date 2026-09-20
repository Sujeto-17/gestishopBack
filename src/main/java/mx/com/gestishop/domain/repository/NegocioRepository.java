package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de la tabla negocios
public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    // Busca un negocio activo (no borrado) por su identificador público
    @Query("SELECT n FROM Negocio n WHERE n.uuidNegocio = :uuidNegocio AND n.deletedAt IS NULL")
    Optional<Negocio> buscarPorUuid(@Param("uuidNegocio") UUID uuidNegocio);

    // Verifica si ya existe un negocio activo con ese correo
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Negocio n WHERE n.correo = :correo AND n.deletedAt IS NULL")
    boolean existeCorreoActivo(@Param("correo") String correo);

    // Lista todos los negocios activos (no borrados), para el panel del superadmin
    @Query("SELECT n FROM Negocio n WHERE n.deletedAt IS NULL ORDER BY n.fechaAlta DESC")
    List<Negocio> listarActivos();
}
