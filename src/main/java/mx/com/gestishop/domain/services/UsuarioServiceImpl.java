package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.RegistrarUsuarioRequestDTO;
import mx.com.gestishop.application.dto.response.SesionResponseDTO;
import mx.com.gestishop.application.mapper.SesionMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.interfaces.UsuarioService;
import mx.com.gestishop.domain.model.AdminNegocio;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Usuario;
import mx.com.gestishop.domain.repository.AdminNegocioRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Implementación del alta de usuarios. Centraliza la creación de
 * superadmin/admin/trabajador para no duplicar la lógica de encriptado
 * y validación de correo en 3 lugares distintos.
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AdminNegocioRepository adminNegocioRepository;
    private final NegocioRepository negocioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SesionResponseDTO registrar(RegistrarUsuarioRequestDTO dto) {

        // Validar que el correo no esté ya en uso por un usuario activo
        boolean correoExiste = RepositoryExecutor.execute(
                () -> usuarioRepository.existeCorreoActivo(dto.getCorreo()),
                "Usuario", "validarCorreo"
        );

        if (correoExiste) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe una cuenta registrada con ese correo."));
        }

        // Si es admin o trabajador, el negocio es obligatorio y debe existir
        Negocio negocio = null;
        if (!"superadmin".equals(dto.getTipoUsuario())) {
            if (dto.getIdNegocio() == null) {
                throw new ApiResponseException(ApiCodeResponse.REQUIRED_DATA,
                        Map.of("detalle", "idNegocio es obligatorio para admin y trabajador."));
            }
            negocio = RepositoryExecutor.execute(
                    () -> negocioRepository.findById(dto.getIdNegocio())
                            .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND,
                                    Map.of("detalle", "El negocio indicado no existe."))),
                    "Negocio", "consultar"
            );
        }

        // Construir la entidad Usuario. La contraseña se encripta aquí,
        //    NUNCA se guarda en texto plano en ningún punto del flujo.
        Usuario usuario = Usuario.builder()
                .tipoUsuario(dto.getTipoUsuario())
                .nombre(dto.getNombre())
                .correo(dto.getCorreo())
                .telefono(dto.getTelefono())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .debeActualizarPassword(true)  // toda cuenta nueva fuerza cambio de contraseña al primer login
                .estatus("activo")
                .build();

        Usuario guardado = RepositoryExecutor.execute(
                () -> usuarioRepository.save(usuario),
                "Usuario", "crear"
        );

        // Si es admin, se crea la relación con su negocio en admin_negocio
        if ("admin".equals(dto.getTipoUsuario())) {
            AdminNegocio relacion = AdminNegocio.builder()
                    .usuario(guardado)
                    .negocio(negocio)
                    .build();

            RepositoryExecutor.executeVoid(
                    () -> adminNegocioRepository.save(relacion),
                    "AdminNegocio", "crear"
            );
        }

        // Nota: si tipoUsuario='trabajador', el alta de la fila en la tabla
        // trabajadores (puesto, turno, salario, nivel_acceso, etc.) se hace
        // en un endpoint aparte (TrabajadorService), ya que requiere más
        // datos laborales que este DTO genérico no contempla.

        return SesionMapper.toResponse(guardado);
    }
}
