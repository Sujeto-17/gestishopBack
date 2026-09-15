package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// Acceso a datos de la tabla usuarios (identidad central: superadmin, admin y trabajador)
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario activo (no borrado lógicamente) por su correo. Usado en el login: se valida credenciales contra este resultado
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.correo = :correo
              AND u.deletedAt IS NULL
            """)
    Optional<Usuario> buscarPorCorreoActivo(@Param("correo") String correo);

    // Busca un usuario por su identificador público (UUID), el que se expone en la API.
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.uuidUsuario = :uuidUsuario
              AND u.deletedAt IS NULL
            """)
    Optional<Usuario> buscarPorUuid(@Param("uuidUsuario") UUID uuidUsuario);

    /**
     * Verifica si ya existe un usuario activo con ese correo.
     * Se usa antes de registrar uno nuevo, para dar un mensaje de negocio claro
     * en vez de esperar a que la BD rechace el INSERT por el índice único.
     */
    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM Usuario u
            WHERE u.correo = :correo
              AND u.deletedAt IS NULL
            """)
    boolean existeCorreoActivo(@Param("correo") String correo);
}
