package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.ContextoSesionDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;
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

/**
 * Implementación del manejo de sesiones vía refresh token.
 * <p>
 * El refresh token viaja al cliente en texto plano, pero en base de datos
 * SOLO se guarda su hash (SHA-256) — así, aunque alguien acceda a la BD,
 * no puede reconstruir tokens válidos ni suplantar sesiones.
 * <p>
 * Implementa rotación: cada vez que se usa un refresh token para renovar,
 * el usado queda revocado y se emite uno nuevo. Si un token YA revocado
 * se intenta reutilizar, es señal de robo (alguien más lo tiene) y se
 * revocan TODAS las sesiones del usuario como medida de contención.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SesionContextResolver sesionContextResolver;

    @Override
    @Transactional
    public String crear(Usuario usuario, String ip, String userAgent) {

        // 1. Generar el token en texto plano (esto es lo único que ve el cliente)
        String tokenPlano = SecureTokenUtil.generar();

        // 2. Guardar solo el HASH en BD, nunca el token real
        String hash = HashUtil.hashHex(tokenPlano, AlgorithmHash.SHA256);

        OffsetDateTime expiracion = OffsetDateTime.now()
                .plusNanos(jwtProperties.getRefreshTokenExpirationMs() * 1_000_000L);

        RefreshToken entidad = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash)
                .ipOrigen(ip)
                .userAgent(userAgent)
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
    @Transactional
    public LoginResponseDTO rotar(String refreshTokenPlano) {
        String hash = HashUtil.hashHex(refreshTokenPlano, AlgorithmHash.SHA256);

        // 1. Buscar la sesión SOLO entre las válidas (no revocadas, no expiradas)
        Optional<RefreshToken> sesionValida = RepositoryExecutor.execute(
                () -> refreshTokenRepository.buscarValidoPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "rotar"
        );

        if (sesionValida.isEmpty()) {
            // 2. No es válida: se busca SIN filtro de estado para distinguir
            //    "robo" (token ya rotado que alguien reutiliza) de un simple
            //    token vencido o inexistente.
            Optional<RefreshToken> sesionCualquiera = RepositoryExecutor.execute(
                    () -> refreshTokenRepository.buscarPorHash(hash),
                    "RefreshToken", "rotar"
            );

            if (sesionCualquiera.isPresent() && Boolean.TRUE.equals(sesionCualquiera.get().getRevocado())) {
                // El token ya fue usado/rotado antes y alguien intenta reutilizarlo:
                // evidencia de robo de sesión. Se revocan TODAS las sesiones del
                // usuario para cortar cualquier acceso no autorizado.
                Long idUsuario = sesionCualquiera.get().getUsuario().getIdUsuario();
                revocarTodasDelUsuario(idUsuario);

                throw new ApiResponseException(ApiCodeResponse.UNAUTHORIZED,
                        Map.of("detalle", "Sesión comprometida detectada. Se cerraron todas tus sesiones activas, "
                                + "vuelve a iniciar sesión."));
            }

            // Token simplemente vencido o inexistente
            throw new ApiResponseException(ApiCodeResponse.UNAUTHORIZED,
                    Map.of("detalle", "La sesión ha expirado. Vuelve a iniciar sesión."));
        }

        RefreshToken sesion = sesionValida.get();
        Usuario usuario = sesion.getUsuario();

        // 3. Rotación: se revoca el token usado ANTES de emitir uno nuevo
        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "rotar"
        );

        // 4. Se vuelve a resolver el contexto (por si cambiaron permisos/negocio
        //    desde el último login) y se emite un nuevo par de tokens
        ContextoSesionDTO contexto = sesionContextResolver.resolver(usuario);

        String nuevoAccessToken = jwtTokenProvider.generarAccessToken(
                usuario, contexto.getIdNegocio(), contexto.getNivelAcceso(), contexto.getModulos());
        String nuevoRefreshToken = crear(usuario, sesion.getIpOrigen(), sesion.getUserAgent());

        return LoginResponseDTO.builder()
                .accessToken(nuevoAccessToken)
                .refreshToken(nuevoRefreshToken)
                .debeActualizarPassword(usuario.getDebeActualizarPassword())
                .tipoUsuario(usuario.getTipoUsuario())
                .nivelAcceso(contexto.getNivelAcceso())
                .idNegocio(contexto.getIdNegocio())
                .modulos(contexto.getModulos())
                .build();
    }

    @Override
    @Transactional
    public void revocar(String refreshTokenPlano) {
        String hash = HashUtil.hashHex(refreshTokenPlano, AlgorithmHash.SHA256);
        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarPorHash(hash, OffsetDateTime.now()),
                "RefreshToken", "revocar"
        );
    }

    @Override
    @Transactional
    public void revocarTodasDelUsuario(Long idUsuario) {
        RepositoryExecutor.executeVoid(
                () -> refreshTokenRepository.revocarTodasDelUsuario(idUsuario, OffsetDateTime.now()),
                "RefreshToken", "revocarTodas"
        );
    }

}
