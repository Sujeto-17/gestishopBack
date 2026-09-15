package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.RolModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Acceso a datos de la tabla rol_modulos (matriz de permisos data-driven)
public interface RolModuloRepository extends JpaRepository<RolModulo, Long> {

    /**
     * Lista los módulos permitidos para una combinación de tipo de usuario
     * y nivel de acceso. Es la consulta central del sistema de permisos:
     * se ejecuta en cada login de un trabajador para armar su lista de módulos.
     */
    @Query("""
            SELECT rm FROM RolModulo rm
            JOIN FETCH rm.modulo m
            WHERE rm.tipoUsuario = :tipoUsuario
              AND rm.nivelAcceso = :nivelAcceso
              AND m.estatus = 'activo'
            ORDER BY m.orden ASC
            """)
    List<RolModulo> buscarPorTipoYNivel(@Param("tipoUsuario") String tipoUsuario,
                                        @Param("nivelAcceso") String nivelAcceso);
}
