package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de la tabla negocios
public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    // Busca un negocio activo (no borrado) por su identificador público
    @Query("SELECT n FROM Negocio n JOIN FETCH n.giro JOIN FETCH n.plan LEFT JOIN FETCH n.municipio WHERE n.uuidNegocio = :uuidNegocio AND n.deletedAt IS NULL")
    Optional<Negocio> buscarPorUuid(@Param("uuidNegocio") UUID uuidNegocio);

    // Verifica si ya existe un negocio activo con ese correo
    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM Negocio n WHERE n.correo = :correo AND n.deletedAt IS NULL")
    boolean existeCorreoActivo(@Param("correo") String correo);

    // Lista todos los negocios activos (no borrados), para el panel del superadmin
    @Query("SELECT n FROM Negocio n JOIN FETCH n.giro JOIN FETCH n.plan LEFT JOIN FETCH n.municipio WHERE n.deletedAt IS NULL ORDER BY n.fechaAlta DESC")
    // Todos los negocios no borrados (en prueba, activos, suspendidos y vencidos). Antes se llamaba listarActivos(),
    // pero nunca filtró por estatus.
    List<Negocio> listarTodos();

    /**
     * Proceso diario: pasa a 'inactivo' los negocios en prueba o activos cuyo vencimiento ya pasó.
     * Es una actualización masiva (una sola consulta); devuelve cuántos negocios cambió.
     * Ojo: una actualización masiva no dispara @PreUpdate, por eso se asigna updatedAt a mano.
     */
    @Modifying
    @Query("""
        UPDATE Negocio n
           SET n.estatus = 'inactivo', n.updatedAt = :ahora
         WHERE n.estatus IN ('prueba', 'activo')
           AND n.vencimiento < :hoy
           AND n.deletedAt IS NULL
        """)
    int desactivarVencidos(@Param("hoy") java.time.LocalDate hoy, @Param("ahora") java.time.OffsetDateTime ahora);
}

