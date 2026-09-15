package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.RegistrarUsuarioRequestDTO;
import mx.com.gestishop.application.dto.response.SesionResponseDTO;

// Contrato de operaciones sobre usuarios (identidad central del sistema)
public interface UsuarioService {

    /**
     * Registra un usuario nuevo (superadmin, admin o trabajador según el DTO).
     * Encripta la contraseña, valida correo único, y si aplica, crea la
     * relación correspondiente (admin_negocio o trabajadores).
     */
    SesionResponseDTO registrar(RegistrarUsuarioRequestDTO dto);
}
