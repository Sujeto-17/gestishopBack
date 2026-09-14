package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final IntentoLoginRepository intentoLoginRepository;
    private final AdminNegocioRepository adminNegocioRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RolModuloRepository rolModuloRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final int MAX_INTENTOS = 5;
    private static final int VENTANA_MINUTOS = 15;

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String ip, String userAgent) {

        validarBloqueoPorIntentos(dto.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreoAndDeletedAtIsNull(dto.getCorreo())
                .orElse(null);

        boolean credencialesValidas = usuario != null
                && passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash());

        registrarIntento(dto.getCorreo(), ip, credencialesValidas);

        if (!credencialesValidas) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Correo o contraseña incorrectos."));
        }

        if (!"activo".equals(usuario.getEstatus())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "La cuenta está inactiva. Contacta al administrador."));
        }

        // Resolver contexto según tipo de usuario: negocio, nivel de acceso y módulos permitidos
        Long idNegocio = null;
        String nivelAcceso = null;
        List<String> modulos;

        switch (usuario.getTipoUsuario()) {
            case "admin" -> {
                AdminNegocio relacion = adminNegocioRepository.findFirstByUsuario_IdUsuario(usuario.getIdUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.CONFLICT,
                                Map.of("detalle", "El usuario admin no tiene negocio asignado.")));
                idNegocio = relacion.getNegocio().getIdNegocio();
                // admin ve TODOS los módulos incluidos en el plan de su negocio
                modulos = relacion.getNegocio().getPlan().getModulos().stream()
                        .map(CatModulo::getNombre).toList();
            }
            case "trabajador" -> {
                Trabajador trabajador = trabajadorRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.CONFLICT,
                                Map.of("detalle", "El trabajador no tiene datos laborales registrados.")));
                idNegocio = trabajador.getNegocio().getIdNegocio();
                nivelAcceso = trabajador.getNivelAcceso();
                modulos = rolModuloRepository.findByTipoUsuarioAndNivelAcceso("trabajador", nivelAcceso)
                        .stream().map(rm -> rm.getModulo().getNombre()).toList();
            }
            default -> modulos = List.of(); // superadmin no necesita lista de módulos, ve todo su propio panel
        }

        usuario.setUltimoAcceso(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        String accessToken = jwtTokenProvider.generarAccessToken(usuario, idNegocio, nivelAcceso, modulos);
        String refreshToken = refreshTokenService.crear(usuario, ip, userAgent);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .debeActualizarPassword(usuario.getDebeActualizarPassword())
                .tipoUsuario(usuario.getTipoUsuario())
                .nivelAcceso(nivelAcceso)
                .idNegocio(idNegocio)
                .modulos(modulos)
                .build();
    }

    @Override
    @Transactional
    public LoginResponseDTO refrescarToken(String refreshTokenPlano) {
        return refreshTokenService.rotar(refreshTokenPlano);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenPlano) {
        refreshTokenService.revocar(refreshTokenPlano);
    }

    @Override
    @Transactional
    public void cambiarPassword(CambiarPasswordRequestDTO dto) {
        Usuario usuario = RepositoryExecutor.execute(
                () -> usuarioRepository.findByUuidUsuario(dto.getUuidUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND)),
                "Usuario", "cambiarPassword"
        );

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "La contraseña actual no es correcta."));
        }

        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuario.setDebeActualizarPassword(false);
        usuarioRepository.save(usuario);

        // Por seguridad, al cambiar contraseña se revocan todas las sesiones activas
        refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());
    }

    private void validarBloqueoPorIntentos(String correo) {
        OffsetDateTime desde = OffsetDateTime.now().minusMinutes(VENTANA_MINUTOS);
        long fallidos = intentoLoginRepository.countByCorreoAndExitosoFalseAndCreatedAtAfter(correo, desde);

        if (fallidos >= MAX_INTENTOS) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Demasiados intentos fallidos. Intenta de nuevo en " + VENTANA_MINUTOS + " minutos."));
        }
    }

    private void registrarIntento(String correo, String ip, boolean exitoso) {
        intentoLoginRepository.save(IntentoLogin.builder()
                .correo(correo).ipOrigen(ip).exitoso(exitoso).build());
    }
}
