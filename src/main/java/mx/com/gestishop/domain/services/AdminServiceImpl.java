package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.AdminResponseDTO;
import mx.com.gestishop.application.mapper.AdminMapper;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.AdminService;
import mx.com.gestishop.domain.interfaces.RefreshTokenService;
import mx.com.gestishop.domain.model.AdminNegocio;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Usuario;
import mx.com.gestishop.domain.repository.AdminNegocioRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.UsuarioRepository;
import mx.com.gestishop.infrastructure.utils.PasswordGeneratorUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de la gestión de administradores. Se apoya en admin_negocio (no directo en usuarios)
 * porque necesita traer el negocio asignado junto con cada admin, dato que la tabla usuarios no tiene.
 * <p>
 * Un administrador (dueño) puede estar en VARIOS negocios o sucursales: cada fila de admin_negocio
 * es una asignación. Reglas que protegen a cada negocio de quedarse sin dueño:
 *  - no se desactiva al último administrador activo de un negocio vigente
 *  - no se desvincula al último administrador activo de un negocio
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminNegocioRepository adminNegocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final PlanLimites planLimites;   // límites del plan (cuántos administradores caben)

    // Una fila por asignación (admin x negocio): un admin con 2 negocios aparece 2 veces
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

    /**
     * Activa o desactiva la CUENTA del admin (afecta a todos sus negocios).
     * Al desactivar se valida que ningún negocio vigente se quede sin administrador activo.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminResponseDTO cambiarEstatus(UUID uuidUsuario) {
        AdminNegocio relacion = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Admin", uuidUsuario)),
                "AdminNegocio", "cambiarEstatus"
        );

        Usuario usuario = relacion.getUsuario();
        boolean desactivando = "activo".equals(usuario.getEstatus());

        if (desactivando) {
            validarQueNingunNegocioQuedeSinAdmin(usuario);
        } else {
            validarLimitePlanAlReactivar(usuario);
        }

        usuario.setEstatus(desactivando ? "inactivo" : "activo");
        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "cambiarEstatus");

        // Al DESACTIVAR la cuenta se cierran todas sus sesiones abiertas (refresh tokens).
        // El access token sigue vivo como máximo 15 min, y ya no se podrá renovar.
        if (desactivando) {
            refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());
        }

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

        // Si alguien más tenía una sesión abierta con la contraseña olvidada, también se invalida
        refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());

        return passwordTemporal;
    }

    // Lista los negocios de un admin (para mostrar "también administra: ..." en la pantalla de admins)
    @Override
    public List<CatalogoRefDTO> listarNegocios(UUID uuidUsuario) {
        Usuario usuario = buscarAdmin(uuidUsuario, "listarNegocios");

        return RepositoryExecutor.execute(
                        () -> adminNegocioRepository.buscarPorUsuarioConNegocio(usuario.getIdUsuario()),
                        "AdminNegocio", "listarNegocios")
                .stream()
                .map(r -> CatalogoRefDTO.builder()
                        .id(r.getNegocio().getIdNegocio().intValue())
                        .nombre(r.getNegocio().getNombre())
                        .build())
                .toList();
    }

    // Asigna un admin existente a otro negocio (mismo dueño, otra sucursal) sin crearle otra cuenta
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminResponseDTO vincularNegocio(UUID uuidUsuario, Long idNegocio) {
        Usuario usuario = buscarAdmin(uuidUsuario, "vincularNegocio");

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "vincularNegocio"
        );

        boolean yaExiste = RepositoryExecutor.execute(
                () -> adminNegocioRepository.existeRelacion(usuario.getIdUsuario(), idNegocio),
                "AdminNegocio", "vincularNegocio");

        if (yaExiste) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Este administrador ya está asignado a ese negocio."));
        }

        // El plan del negocio destino puede limitar cuántos administradores activos tiene
        if ("activo".equals(usuario.getEstatus())) {
            planLimites.validarAgregarAdministrador(negocio);
        }

        AdminNegocio relacion = AdminNegocio.builder().usuario(usuario).negocio(negocio).build();
        AdminNegocio guardada = RepositoryExecutor.execute(
                () -> adminNegocioRepository.save(relacion), "AdminNegocio", "vincularNegocio");

        return AdminMapper.toResponse(guardada);
    }

    /**
     * Quita a un admin de un negocio. Se cierran sus sesiones para que no siga trabajando con un
     * access token de ese negocio (tendrá que volver a entrar y verá solo los que le quedan).
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void desvincularNegocio(UUID uuidUsuario, Long idNegocio) {
        AdminNegocio relacion = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarRelacion(uuidUsuario, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("AdminNegocio", idNegocio)),
                "AdminNegocio", "desvincularNegocio"
        );

        Usuario usuario = relacion.getUsuario();
        Negocio negocio = relacion.getNegocio();

        List<AdminNegocio> susNegocios = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUsuarioConNegocio(usuario.getIdUsuario()),
                "AdminNegocio", "desvincularNegocio");

        if (susNegocios.size() <= 1) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Es el único negocio de este administrador. Para quitarle el acceso, desactiva su cuenta."));
        }

        if ("activo".equals(usuario.getEstatus()) && !"inactivo".equals(negocio.getEstatus())) {
            long otros = RepositoryExecutor.execute(
                    () -> adminNegocioRepository.contarOtrosAdminsActivos(negocio.getIdNegocio(), usuario.getIdUsuario()),
                    "AdminNegocio", "desvincularNegocio");

            if (otros == 0) {
                throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                        Map.of("detalle", "Es el único administrador activo de «" + negocio.getNombre()
                                + "». Agrega otro administrador a ese negocio antes de quitarlo."));
            }
        }

        RepositoryExecutor.executeVoid(() -> adminNegocioRepository.delete(relacion), "AdminNegocio", "desvincularNegocio");
        refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());
    }

    // ------------------------------------------------------------------ internos

    // Busca un usuario por uuid y exige que sea de tipo admin
    private Usuario buscarAdmin(UUID uuidUsuario, String operacion) {
        Usuario usuario = RepositoryExecutor.execute(
                () -> usuarioRepository.buscarPorUuid(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Admin", uuidUsuario)),
                "Usuario", operacion
        );

        if (!"admin".equals(usuario.getTipoUsuario())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "La cuenta indicada no es de un administrador."));
        }
        return usuario;
    }

    /**
     * Antes de desactivar una cuenta: revisa cada negocio vigente del admin y comprueba que le quede al
     * menos OTRO administrador activo. Los negocios ya suspendidos no se protegen (no hay nada que cuidar).
     */
    private void validarQueNingunNegocioQuedeSinAdmin(Usuario usuario) {
        List<AdminNegocio> relaciones = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUsuarioConNegocio(usuario.getIdUsuario()),
                "AdminNegocio", "validarUltimoAdmin");

        List<String> sinRelevo = new ArrayList<>();

        for (AdminNegocio relacion : relaciones) {
            Negocio negocio = relacion.getNegocio();
            if ("inactivo".equals(negocio.getEstatus())) continue;

            long otros = RepositoryExecutor.execute(
                    () -> adminNegocioRepository.contarOtrosAdminsActivos(negocio.getIdNegocio(), usuario.getIdUsuario()),
                    "AdminNegocio", "validarUltimoAdmin");

            if (otros == 0) sinRelevo.add("«" + negocio.getNombre() + "»");
        }

        if (!sinRelevo.isEmpty()) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "No se puede desactivar: es el único administrador activo de "
                            + String.join(", ", sinRelevo)
                            + ". Agrega otro administrador a esos negocios o suspende los negocios."));
        }
    }

    // Al reactivar una cuenta de admin, ocupa un lugar en cada negocio donde está asignada
    private void validarLimitePlanAlReactivar(Usuario usuario) {
        List<AdminNegocio> relaciones = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUsuarioConNegocio(usuario.getIdUsuario()),
                "AdminNegocio", "validarLimitePlan");

        for (AdminNegocio relacion : relaciones) {
            planLimites.validarAgregarAdministrador(relacion.getNegocio());
        }
    }
}
