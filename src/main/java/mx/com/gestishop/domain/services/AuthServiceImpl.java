package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.ContextoSesionDTO;
import mx.com.gestishop.application.dto.request.CambiarPasswordRequestDTO;
import mx.com.gestishop.application.dto.request.LoginRequestDTO;
import mx.com.gestishop.application.dto.response.LoginResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.interfaces.AuthService;
import mx.com.gestishop.domain.interfaces.RefreshTokenService;
import mx.com.gestishop.domain.model.*;
import mx.com.gestishop.domain.repository.*;
import mx.com.gestishop.infrastructure.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Implementación del servicio de autenticación.
 * <p>
 * Orquesta el flujo completo de login: validación de credenciales, bloqueo
 * por intentos fallidos (fuerza bruta), resolución del contexto del usuario
 * (a qué negocio pertenece, qué módulos puede ver) y emisión de tokens.
 * <p>
 * También maneja logout, renovación de sesión (delegado a RefreshTokenService)
 * y cambio de contraseña.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final IntentoLoginRepository intentoLoginRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final SesionContextResolver sesionContextResolver;

    // Umbral de intentos fallidos antes de bloquear temporalmente el login de un correo.
    private static final int MAX_INTENTOS = 5;
    // Ventana de tiempo (en minutos) sobre la que se cuentan los intentos fallidos.
    private static final int VENTANA_MINUTOS = 15;

    /**
     * Valida credenciales y, si son correctas, resuelve el contexto del usuario
     * (negocio, nivel de acceso, módulos permitidos) y emite un par de tokens.
     *
     * @param dto       correo y contraseña enviados por el cliente
     * @param ip        IP de origen de la petición, se guarda para auditoría del refresh token
     * @param userAgent User-Agent del navegador/app que hace login, mismo propósito de auditoría
     */
    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String ip, String userAgent) {

        // 1. Verificar que el correo no esté bloqueado por demasiados intentos fallidos recientes
        validarBloqueoPorIntentos(dto.getCorreo());

        // 2. Buscar el usuario por correo (solo activos, deletedAt IS NULL ya filtrado en el repositorio)
        Usuario usuario = RepositoryExecutor.execute(
                () -> usuarioRepository.buscarPorCorreoActivo(dto.getCorreo()).orElse(null),
                "Usuario", "login"
        );

        // 3. Validar la contraseña con BCrypt. Se usa un solo booleano combinado
        //    (usuario nulo O contraseña incorrecta) para no revelar si el correo existe o no
        //    — evita que un atacante use el login para enumerar correos válidos.
        boolean credencialesValidas = usuario != null
                && passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash());

        // 4. Registrar el intento (exitoso o fallido) SIEMPRE, para alimentar el rate limiting
        registrarIntento(dto.getCorreo(), ip, credencialesValidas);

        if (!credencialesValidas) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Correo o contraseña incorrectos."));
        }

        if (!"activo".equals(usuario.getEstatus())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "La cuenta está inactiva. Contacta al administrador."));
        }

        // 5. Resolver contexto: a qué negocio pertenece, nivel de acceso, módulos permitidos.
        // La lógica vive en SesionContextResolver, compartida con RefreshTokenServiceImpl.
        ContextoSesionDTO contexto = sesionContextResolver.resolver(usuario);

        // 6. Registrar el momento del acceso exitoso
        usuario.setUltimoAcceso(OffsetDateTime.now());
        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "actualizarUltimoAcceso");

        // 7. Emitir el access token (JWT corto) y el refresh token (opaco, revocable, largo)
        String accessToken = jwtTokenProvider.generarAccessToken(
                usuario, contexto.getIdNegocio(), contexto.getNivelAcceso(), contexto.getModulos());
        String refreshToken = refreshTokenService.crear(usuario, ip, userAgent);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .debeActualizarPassword(usuario.getDebeActualizarPassword())
                .tipoUsuario(usuario.getTipoUsuario())
                .nivelAcceso(contexto.getNivelAcceso())
                .idNegocio(contexto.getIdNegocio())
                .modulos(contexto.getModulos())
                .build();
    }

    /**
     * Renueva la sesión intercambiando un refresh token válido por un nuevo
     * par access/refresh. La lógica de rotación vive en RefreshTokenService.
     */
    @Override
    @Transactional
    public LoginResponseDTO refrescarToken(String refreshTokenPlano) {
        return refreshTokenService.rotar(refreshTokenPlano);
    }

    /**
     * Cierra sesión revocando el refresh token indicado.
     * El access token sigue siendo válido hasta que expire por sí solo
     * (máximo 15 min), por eso su vida útil es intencionalmente corta.
     */
    @Override
    @Transactional
    public void logout(String refreshTokenPlano) {
        refreshTokenService.revocar(refreshTokenPlano);
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * Requiere validar la contraseña actual antes de permitir el cambio.
     * Al finalizar, revoca TODAS las sesiones activas del usuario como
     * medida de seguridad (por si el cambio se debe a una sospecha de robo).
     */
    @Override
    @Transactional
    public void cambiarPassword(CambiarPasswordRequestDTO dto) {
        Usuario usuario = RepositoryExecutor.execute(
                () -> usuarioRepository.buscarPorUuid(dto.getUuidUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND)),
                "Usuario", "cambiarPassword"
        );

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "La contraseña actual no es correcta."));
        }

        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuario.setDebeActualizarPassword(false);
        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "cambiarPassword");

        // Por seguridad, cambiar la contraseña invalida cualquier sesión abierta
        // en otros dispositivos (obliga a volver a iniciar sesión en todos lados).
        refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());
    }

    /**
     * Verifica si el correo ha superado el número máximo de intentos fallidos
     * dentro de la ventana de tiempo definida. Si es así, bloquea el login
     * temporalmente, incluso si la contraseña ahora sea correcta — es la
     * defensa principal contra ataques de fuerza bruta.
     */
    private void validarBloqueoPorIntentos(String correo) {
        OffsetDateTime desde = OffsetDateTime.now().minusMinutes(VENTANA_MINUTOS);

        long fallidos = RepositoryExecutor.execute(
                () -> intentoLoginRepository.contarFallidosDesde(correo, desde),
                "IntentoLogin", "validarBloqueo"
        );

        if (fallidos >= MAX_INTENTOS) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Demasiados intentos fallidos. Intenta de nuevo en "
                            + VENTANA_MINUTOS + " minutos."));
        }
    }

    /**
     * Registra un intento de login (exitoso o fallido) en la bitácora.
     * Esta tabla alimenta tanto el bloqueo por fuerza bruta como, a futuro,
     * posibles reportes de seguridad (intentos sospechosos por IP, etc).
     */
    private void registrarIntento(String correo, String ip, boolean exitoso) {
        RepositoryExecutor.executeVoid(
                () -> intentoLoginRepository.save(
                        IntentoLogin.builder()
                                .correo(correo)
                                .ipOrigen(ip)
                                .exitoso(exitoso)
                                .build()
                ),
                "IntentoLogin", "registrar"
        );
    }
}