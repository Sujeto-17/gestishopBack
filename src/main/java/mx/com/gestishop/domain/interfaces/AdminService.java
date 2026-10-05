package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.response.AdminResponseDTO;

import java.util.List;
import java.util.UUID;

// Contrato de gestión de administradores (consulta, activación y reseteo de contraseña)
public interface AdminService {

    List<AdminResponseDTO> listarTodos();

    AdminResponseDTO obtenerPorUuid(UUID uuidUsuario);

    // Activa o desactiva la cuenta de acceso de un administrador
    AdminResponseDTO cambiarEstatus(UUID uuidUsuario);

    /**
     * Genera una contraseña temporal aleatoria, la encripta y la asigna al admin,
     * forzando el cambio de contraseña en su próximo login. Revoca sus sesiones
     * activas por seguridad.
     *
     * @return la contraseña temporal EN TEXTO PLANO, única vez que se expone —
     *         el superadmin debe comunicarla al admin por un canal seguro
     *         (no queda registrada en ningún lado después de esta respuesta).
     */
    String restablecerPassword(UUID uuidUsuario);
}
