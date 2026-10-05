package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.TrabajadorActualizarRequestDTO;
import mx.com.gestishop.application.dto.request.TrabajadorRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorResponseDTO;
import mx.com.gestishop.application.mapper.TrabajadorMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.RefreshTokenService;
import mx.com.gestishop.domain.interfaces.TrabajadorService;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Trabajador;
import mx.com.gestishop.domain.model.Usuario;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.TrabajadorRepository;
import mx.com.gestishop.domain.repository.UsuarioRepository;
import mx.com.gestishop.infrastructure.utils.PasswordGeneratorUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación del alta y gestión de trabajadores. Orquesta dos tablas
 * relacionadas (usuarios + trabajadores) en una sola transacción: si algo
 * falla a mitad del proceso, ninguna de las dos filas queda huérfana.
 */
@Service
@RequiredArgsConstructor
public class TrabajadorServiceImpl implements TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorResponseDTO crear(TrabajadorRequestDTO dto) {

        // Validar correo único entre usuarios activos
        boolean correoExiste = RepositoryExecutor.execute(
                () -> usuarioRepository.existeCorreoActivo(dto.getCorreo()),
                "Usuario", "validarCorreo"
        );

        if (correoExiste) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe una cuenta registrada con ese correo."));
        }

        // El negocio debe existir
        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(dto.getIdNegocio())
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", dto.getIdNegocio())),
                "Negocio", "consultar"
        );

        // Crear la identidad de acceso (tabla usuarios). Password encriptado aquí,
        //    nunca se persiste en texto plano en ningún punto del flujo.
        Usuario usuario = TrabajadorMapper.toUsuarioEntity(dto, passwordEncoder.encode(dto.getPassword()));
        Usuario usuarioGuardado = RepositoryExecutor.execute(
                () -> usuarioRepository.save(usuario), "Usuario", "crear");

        // Crear el registro laboral (tabla trabajadores), ligado al usuario y negocio anteriores
        Trabajador trabajador = TrabajadorMapper.toTrabajadorEntity(dto, usuarioGuardado, negocio);
        Trabajador guardado = RepositoryExecutor.execute(
                () -> trabajadorRepository.save(trabajador), "Trabajador", "crear");

        // Se vuelve a asociar el usuario ya guardado, para que el mapper de respuesta
        // tenga todos los datos completos (uuid, fechas, etc.)
        guardado.setUsuario(usuarioGuardado);

        return TrabajadorMapper.toResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorResponseDTO actualizar(UUID uuidUsuario, TrabajadorActualizarRequestDTO dto) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "actualizar"
        );

        // Datos de acceso (nombre/teléfono se pueden editar; correo y password NO se tocan aquí
        // — el cambio de correo o password requiere flujos propios por seguridad)
        Usuario usuario = entidad.getUsuario();
        TrabajadorMapper.actualizarEntidad(entidad, usuario, dto);
        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "actualizar");

        Trabajador actualizado = RepositoryExecutor.execute(
                () -> trabajadorRepository.save(entidad), "Trabajador", "actualizar");
        actualizado.setUsuario(usuario);

        return TrabajadorMapper.toResponse(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorResponseDTO cambiarEstatus(UUID uuidUsuario) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "cambiarEstatus"
        );

        // El estatus vive en usuarios (activo/inactivo controla el acceso al sistema),
        // no en trabajadores (esa tabla no tiene columna estatus propia).
        Usuario usuario = entidad.getUsuario();
        usuario.setEstatus("activo".equals(usuario.getEstatus()) ? "inactivo" : "activo");

        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "cambiarEstatus");
        entidad.setUsuario(usuario);

        return TrabajadorMapper.toResponse(entidad);
    }

    @Override
    public TrabajadorResponseDTO obtenerPorUuid(UUID uuidUsuario) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "consultar"
        );
        return TrabajadorMapper.toResponse(entidad);
    }

    @Override
    public List<TrabajadorResponseDTO> listarPorNegocio(Long idNegocio) {
        return RepositoryExecutor.execute(
                        () -> trabajadorRepository.listarPorNegocio(idNegocio), "Trabajador", "listarPorNegocio")
                .stream().map(TrabajadorMapper::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String restablecerPassword(UUID uuidUsuario) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuario(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "restablecerPassword"
        );

        Usuario usuario = entidad.getUsuario();
        String passwordTemporal = PasswordGeneratorUtil.generar();

        usuario.setPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeActualizarPassword(true);

        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "restablecerPassword");
        refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());

        return passwordTemporal;
    }
}
