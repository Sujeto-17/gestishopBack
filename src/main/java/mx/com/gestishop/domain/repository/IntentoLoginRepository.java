package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.IntentoLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

// Acceso a datos de la tabla intentos_login (rate limiting de fuerza bruta)
public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {

    /**
     * Cuenta cuántos intentos FALLIDOS hubo para un correo desde cierta fecha.
     * Si supera el umbral definido en AuthServiceImpl, se bloquea el login temporalmente.
     */
    @Query("""
            SELECT COUNT(il) FROM IntentoLogin il
            WHERE il.correo = :correo
              AND il.exitoso = false
              AND il.createdAt > :desde
            """)
    long contarFallidosDesde(@Param("correo") String correo, @Param("desde") OffsetDateTime desde);
}
