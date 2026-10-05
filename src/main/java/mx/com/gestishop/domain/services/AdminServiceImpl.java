package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.AdminResponseDTO;
import mx.com.gestishop.application.mapper.AdminMapper;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.AdminService;
import mx.com.gestishop.domain.interfaces.RefreshTokenService;
import mx.com.gestishop.domain.model.AdminNegocio;
import mx.com.gestishop.domain.model.Usuario;
import mx.com.gestishop.domain.repository.AdminNegocioRepository;
import mx.com.gestishop.domain.repository.UsuarioRepository;
import mx.com.gestishop.infrastructure.utils.PasswordGeneratorUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementación de la gestión de administradores. Se apoya en admin_negocio
 * (no directo en usuarios) porque necesita traer el negocio asignado junto
 * con cada admin, dato que la tabla usuarios por sí sola no tiene.
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminNegocioRepository adminNegocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<AdminResponseDTO> listarTodos() {
        return RepositoryExecutor.execute(adminNegocioRepository::listarTodos, "AdminNegocio", "listarTodos")
                .stream().map(AdminMapper::toResponse).toList();
    }

    @Override
    public AdminResponseDTO obtenerPorUuid(UUID uuidUsuario) {
        AdminNegocio relacion = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Admin", uuidUsuario)),
                "AdminNegocio", "consultar"
        );
        return AdminMapper.toResponse(relacion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminResponseDTO cambiarEstatus(UUID uuidUsuario) {
        AdminNegocio relacion = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Admin", uuidUsuario)),
                "AdminNegocio", "cambiarEstatus"
        );

        Usuario usuario = relacion.getUsuario();
        usuario.setEstatus("activo".equals(usuario.getEstatus()) ? "inactivo" : "activo");

        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "cambiarEstatus");
        relacion.setUsuario(usuario);

        return AdminMapper.toResponse(relacion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String restablecerPassword(UUID uuidUsuario) {
        AdminNegocio relacion = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Admin", uuidUsuario)),
                "AdminNegocio", "restablecerPassword"
        );

        Usuario usuario = relacion.getUsuario();
        String passwordTemporal = PasswordGeneratorUtil.generar();

        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeActualizarPassword(true); // obliga a cambiarla en el próximo login

        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "restablecerPassword");

        // Por seguridad, si alguien más tenía una sesión abierta con la contraseña
        // olvidada, esa sesión también se invalida al resetear.
        refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());

        return passwordTemporal;
    }
}
