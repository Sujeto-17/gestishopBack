package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.ContextoSesionDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;
import mx.com.gestishop.application.mapper.SesionMapper;
import mx.com.gestishop.core.enums.AlgorithmHash;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.utils.HashUtil;
import mx.com.gestishop.domain.interfaces.RefreshTokenService;
import mx.com.gestishop.domain.model.RefreshToken;
import mx.com.gestishop.domain.model.Usuario;
import mx.com.gestishop.domain.repository.RefreshTokenRepository;
import mx.com.gestishop.infrastructure.jwt.JwtProperties;
import mx.com.gestishop.infrastructure.jwt.JwtTokenProvider;
import mx.com.gestishop.infrastructure.utils.SecureTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del manejo de sesiones vía refresh token.
 * <p>
 * El refresh token viaja al cliente en texto plano, pero en base de datos SOLO se guarda su hash
 * (SHA-256): aunque alguien acceda a la BD, no puede reconstruir tokens válidos ni suplantar sesiones.
 * <p>
 * Implementa rotación: cada vez que se usa un refresh token para renovar, el usado queda revocado y
 * se emite uno nuevo. Si un token YA revocado se intenta reutilizar, es señal de robo y se revocan
 * TODAS las sesiones del usuario como medida de contención.
 * <p>
 * Para los administradores con varios negocios, la sesión recuerda el negocio activo
 * (refresh_tokens.id_negocio_activo) y lo conserva en cada rotación.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SesionContextResolver sesionContextResolver;

    // Crea una sesión sin negocio activo (superadmin y trabajadores)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String crear(Usuario usuario, String ip, String userAgent) {
        return crear(usuario, ip, userAgent, null);
    }

    /**
     * Crea una sesión. Solo se guarda el HASH en BD, nunca el token real (que es lo único que ve el cliente).
     *
     * @param idNegocioActivo negocio con el que trabaja el admin en esta sesión (null para los demás)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String crear(Usuario usuario, String ip, String userAgent, Long idNegocioActivo) {
        String tokenPlano = SecureTokenUtil.generar();
        String hash = HashUtil.hashHex(tokenPlano, AlgorithmHash.SHA256);

        OffsetDateTime expiracion = OffsetDateTime.now()
                .plusNanos(jwtProperties.getRefreshTokenExpirationMs() * 1_000_000L);

        RefreshToken entidad = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash)
                .ipOrigen(ip)
                .userAgent(userAgent)
                .idNegocioActivo(idNegocioActivo)
                .fechaExpiracion(expiracion)
                .revocado(false)
                .build();

        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.save(entidad),
                "RefreshToken", "crear"
        );

        return tokenPlano;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ApiResponseException.class)
    public LoginResponseDTO rotar(String refreshTokenPlano) {
        String hash = HashUtil.hashHex(refreshTokenPlano, AlgorithmHash.SHA256);

        // Buscar la sesión SOLO entre las válidas (no revocadas, no expiradas)
        Optional<RefreshToken> sesionValida = RepositoryExecutor.execute(
                () -> refreshTokenRepository.buscarValidoPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "rotar"
        );

        if (sesionValida.isEmpty()) {
            // No es válida: se busca SIN filtro de estado para distinguir "robo" (token ya rotado
            // que alguien reutiliza) de un simple token vencido o inexistente.
            Optional<RefreshToken> sesionCualquiera = RepositoryExecutor.execute(
                    () -> refreshTokenRepository.buscarPorHash(hash),
                    "RefreshToken", "rotar"
            );

            if (sesionCualquiera.isPresent() && Boolean.TRUE.equals(sesionCualquiera.get().getRevocado())) {
                // El token ya fue usado/rotado antes y alguien intenta reutilizarlo: posible robo de sesión.
                revocarTodasDelUsuario(sesionCualquiera.get().getUsuario().getIdUsuario());

                throw new ApiResponseException(ApiCodeResponse.UNAUTHORIZED,
                        Map.of("detalle", "Sesión comprometida detectada. Se cerraron todas tus sesiones activas, "
                                + "vuelve a iniciar sesión."));
            }

            throw new ApiResponseException(ApiCodeResponse.UNAUTHORIZED,
                    Map.of("detalle", "La sesión ha expirado. Vuelve a iniciar sesión."));
        }

        RefreshToken sesion = sesionValida.get();
        Usuario usuario = sesion.getUsuario();

        // Un usuario desactivado ya no puede renovar su sesión
        if (!"activo".equals(usuario.getEstatus())) {
            revocarTodasDelUsuario(usuario.getIdUsuario());
            throw new ApiResponseException(ApiCodeResponse.UNAUTHORIZED,
                    Map.of("detalle", "La cuenta está inactiva. Contacta al administrador."));
        }

        // Rotación: se revoca el token usado ANTES de emitir uno nuevo
        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "rotar"
        );

        // Se vuelve a resolver el contexto (por si cambiaron permisos, plan o vigencia). Un admin conserva
        // el negocio que tenía activo; si ya no está disponible, cae al primero disponible.
        ContextoSesionDTO contexto = sesionContextResolver.resolver(usuario, sesion.getIdNegocioActivo());

        return emitirSesion(usuario, contexto, sesion.getIpOrigen(), sesion.getUserAgent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ApiResponseException.class)
    public LoginResponseDTO cambiarNegocio(String refreshTokenPlano, UUID uuidUsuario, Long idNegocio) {
        String hash = HashUtil.hashHex(refreshTokenPlano, AlgorithmHash.SHA256);

        RefreshToken sesion = RepositoryExecutor.execute(
                () -> refreshTokenRepository.buscarValidoPorHash(hash, OffsetDateTime.now())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.UNAUTHORIZED,
                                Map.of("detalle", "La sesión ha expirado. Vuelve a iniciar sesión."))),
                "RefreshToken", "cambiarNegocio"
        );

        Usuario usuario = sesion.getUsuario();

        // El refresh token debe ser de la MISMA cuenta que está autenticada con el access token
        if (!usuario.getUuidUsuario().equals(uuidUsuario)) {
            throw new ApiResponseException(ApiCodeResponse.FORBIDDEN);
        }

        if (!"admin".equals(usuario.getTipoUsuario())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Solo los administradores pueden cambiar de negocio."));
        }

        // Estricto: el negocio debe ser de este admin y estar vigente (si no, falla con el motivo)
        ContextoSesionDTO contexto = sesionContextResolver.resolverEstricto(usuario, idNegocio);

        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "cambiarNegocio"
        );

        return emitirSesion(usuario, contexto, sesion.getIpOrigen(), sesion.getUserAgent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revocar(String refreshTokenPlano) {
        String hash = HashUtil.hashHex(refreshTokenPlano, AlgorithmHash.SHA256);
        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "revocar"
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revocarTodasDelUsuario(Long idUsuario) {
        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarTodasDelUsuario(idUsuario, OffsetDateTime.now()),
                "RefreshToken", "revocarTodas"
        );
    }

    // Emite el par de tokens de una sesión ya validada (compartido por rotar y cambiarNegocio)
    private LoginResponseDTO emitirSesion(Usuario usuario, ContextoSesionDTO contexto, String ip, String userAgent) {
        String accessToken = jwtTokenProvider.generarAccessToken(
                usuario, contexto.getIdNegocio(), contexto.getNivelAcceso(), contexto.getModulos());

        // Solo el admin guarda su negocio activo en la sesión
        Long negocioActivo = "admin".equals(usuario.getTipoUsuario()) ? contexto.getIdNegocio() : null;
        String refreshToken = crear(usuario, ip, userAgent, negocioActivo);

        return SesionMapper.toLoginResponse(usuario, contexto, accessToken, refreshToken);
    }
}