package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.TrabajadorBaseDTO;
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
import mx.com.gestishop.domain.repository.*;
import mx.com.gestishop.infrastructure.utils.PasswordGeneratorUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;

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
    private final CatGeneroRepository catGeneroRepository;
    private final CatEstadoCivilRepository catEstadoCivilRepository;
    private final CatMunicipioRepository catMunicipioRepository;

    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenService refreshTokenService;
    private final PlanLimites planLimites;   // límites del plan (cuántos trabajadores caben)

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorResponseDTO crear(TrabajadorRequestDTO dto, Long idNegocio) {

        validarRegimenYPago(dto);

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
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar"
        );

        // El plan del negocio puede limitar cuántos trabajadores activos tiene
        planLimites.validarAgregarTrabajador(negocio);

        // Crear la identidad de acceso (tabla usuarios). Password encriptado aquí,
        //    nunca se persiste en texto plano en ningún punto del flujo.
        Usuario usuario = TrabajadorMapper.toUsuarioEntity(dto, passwordEncoder.encode(dto.getPassword()));
        Usuario usuarioGuardado = RepositoryExecutor.execute(
                () -> usuarioRepository.save(usuario), "Usuario", "crear");

        // Crear el registro laboral (tabla trabajadores), ligado al usuario y negocio anteriores
        Trabajador trabajador = TrabajadorMapper.toTrabajadorEntity(dto, usuarioGuardado, negocio);
        asignarCatalogos(trabajador, dto);

        Trabajador guardado = RepositoryExecutor.execute(
                () -> trabajadorRepository.save(trabajador), "Trabajador", "crear");

        // Se vuelve a asociar el usuario ya guardado, para que el mapper de respuesta
        // tenga todos los datos completos (uuid, fechas, etc.)
        guardado.setUsuario(usuarioGuardado);

        return TrabajadorMapper.toResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorResponseDTO actualizar(UUID uuidUsuario, TrabajadorActualizarRequestDTO dto, Long idNegocio) {

        validarRegimenYPago(dto);

        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuarioYNegocio(uuidUsuario, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "actualizar"
        );

        // Datos de acceso (nombre/teléfono se pueden editar; correo y password NO se tocan aquí
        // — el cambio de correo o password requiere flujos propios por seguridad)
        Usuario usuario = entidad.getUsuario();
        TrabajadorMapper.actualizarEntidad(entidad, usuario, dto);
        asignarCatalogos(entidad, dto);

        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "actualizar");

        Trabajador actualizado = RepositoryExecutor.execute(
                () -> trabajadorRepository.save(entidad), "Trabajador", "actualizar");
        actualizado.setUsuario(usuario);

        return TrabajadorMapper.toResponse(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrabajadorResponseDTO cambiarEstatus(UUID uuidUsuario, Long idNegocio) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuarioYNegocio(uuidUsuario, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Trabajador", uuidUsuario)),
                "Trabajador", "cambiarEstatus"
        );

        // El estatus vive en usuarios (activo/inactivo controla el acceso al sistema),
        // no en trabajadores (esa tabla no tiene columna estatus propia).
        Usuario usuario = entidad.getUsuario();

        // Reactivar a un trabajador también ocupa un lugar del plan
        if ("inactivo".equals(usuario.getEstatus())) {
            planLimites.validarAgregarTrabajador(entidad.getNegocio());
        }

        usuario.setEstatus("activo".equals(usuario.getEstatus()) ? "inactivo" : "activo");

        RepositoryExecutor.executeVoid(() -> usuarioRepository.save(usuario), "Usuario", "cambiarEstatus");

        // Al DESACTIVAR la cuenta se cierran todas sus sesiones abiertas (refresh tokens).
        // El access token sigue vivo como máximo 15 min, y ya no se podrá renovar.
        if ("inactivo".equals(usuario.getEstatus())) {
            refreshTokenService.revocarTodasDelUsuario(usuario.getIdUsuario());
        }
        entidad.setUsuario(usuario);

        return TrabajadorMapper.toResponse(entidad);
    }

    @Override
    public TrabajadorResponseDTO obtenerPorUuid(UUID uuidUsuario, Long idNegocio) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuarioYNegocio(uuidUsuario, idNegocio)
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
    public String restablecerPassword(UUID uuidUsuario, Long idNegocio) {
        Trabajador entidad = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUuidUsuarioYNegocio(uuidUsuario, idNegocio)
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

    /**
     * Reglas que dependen de otros campos y que las anotaciones no pueden expresar:
     * - régimen formal exige CURP, NSS, RFC, SDI y fecha de alta en el IMSS
     * - pago por transferencia exige banco y CLABE
     * Se reportan todos los faltantes juntos en un solo mensaje.
     */
    private void validarRegimenYPago(TrabajadorBaseDTO dto) {
        List<String> faltantes = new ArrayList<>();

        if ("formal".equals(dto.getRegimen())) {
            if (!StringUtils.hasText(dto.getCurp())) faltantes.add("CURP");
            if (!StringUtils.hasText(dto.getNss())) faltantes.add("NSS");
            if (!StringUtils.hasText(dto.getRfc())) faltantes.add("RFC");
            if (dto.getSalarioDiarioIntegrado() == null) faltantes.add("salario diario integrado");
            if (dto.getFechaAltaImss() == null) faltantes.add("fecha de alta en el IMSS");
        }

        if ("transferencia".equals(dto.getMetodoPago())) {
            if (!StringUtils.hasText(dto.getBanco())) faltantes.add("banco");
            if (!StringUtils.hasText(dto.getClabe())) faltantes.add("CLABE");
        }

        if (!faltantes.isEmpty()) {
            throw new ApiResponseException(ApiCodeResponse.REQUIRED_DATA,
                    Map.of("detalle", "Faltan datos obligatorios: " + String.join(", ", faltantes) + "."));
        }
    }

    // Métodos privados nuevos (imports: java.util.Optional, java.util.function.Function):
    private void asignarCatalogos(Trabajador t, TrabajadorBaseDTO dto) {
        t.setGenero(cargarOpcional(dto.getIdGenero(), catGeneroRepository::findById, "CatGenero"));
        t.setEstadoCivil(cargarOpcional(dto.getIdEstadoCivil(), catEstadoCivilRepository::findById, "CatEstadoCivil"));
        t.setMunicipio(cargarOpcional(dto.getIdMunicipio(), catMunicipioRepository::findById, "CatMunicipio"));
    }

    private <T> T cargarOpcional(Long id, Function<Long, Optional<T>> buscar, String entidad) {
        if (id == null) return null;
        return RepositoryExecutor.execute(
                () -> buscar.apply(id).orElseThrow(() -> ErrorFactory.notFound(entidad, id)),
                entidad, "consultar");
    }
}

