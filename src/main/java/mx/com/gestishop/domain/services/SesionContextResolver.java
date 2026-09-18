package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.ContextoSesionDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.model.AdminNegocio;
import mx.com.gestishop.domain.model.CatModulo;
import mx.com.gestishop.domain.model.Trabajador;
import mx.com.gestishop.domain.model.Usuario;
import mx.com.gestishop.domain.repository.AdminNegocioRepository;
import mx.com.gestishop.domain.repository.RolModuloRepository;
import mx.com.gestishop.domain.repository.TrabajadorRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Resuelve el contexto de sesión de un usuario ya autenticado: a qué negocio
 * pertenece, su nivel de acceso y qué módulos puede ver.
 * <p>
 * Se usa tanto en el login inicial como al renovar la sesión con un refresh
 * token, para no repetir el mismo switch por tipo de usuario en dos lugares.
 * Cualquier cambio en las reglas de permisos se hace UNA sola vez, aquí.
 */
@Component
@RequiredArgsConstructor
public class SesionContextResolver {

    private final AdminNegocioRepository adminNegocioRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RolModuloRepository rolModuloRepository;

    /**
     * Determina el contexto según el tipo de usuario:
     * - superadmin: sin negocio, sin restricción de módulos (su panel muestra todo).
     * - admin: pertenece a un negocio, ve TODOS los módulos del plan de ese negocio.
     * - trabajador: pertenece a un negocio, ve solo los módulos de su nivel de acceso.
     */
    public ContextoSesionDTO resolver(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case "admin" -> resolverAdmin(usuario);
            case "trabajador" -> resolverTrabajador(usuario);
            default -> ContextoSesionDTO.builder()
                    .idNegocio(null)
                    .nivelAcceso(null)
                    .modulos(List.of())
                    .build();
        };
    }

    private ContextoSesionDTO resolverAdmin(Usuario usuario) {
        // Un admin puede tener más de un negocio (admin_negocio es N:M),
        // para login/refresh se toma el primero registrado.
        AdminNegocio relacion = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPrimerNegocioDeUsuario(usuario.getIdUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.CONFLICT,
                                Map.of("detalle", "El usuario admin no tiene negocio asignado."))),
                "AdminNegocio", "resolverContexto"
        );

        List<String> modulos = relacion.getNegocio().getPlan().getModulos().stream()
                .map(CatModulo::getNombre)
                .toList();

        return ContextoSesionDTO.builder()
                .idNegocio(relacion.getNegocio().getIdNegocio())
                .nivelAcceso(null)  // admin no tiene nivel_acceso, ese campo es exclusivo de trabajador
                .modulos(modulos)
                .build();
    }

    private ContextoSesionDTO resolverTrabajador(Usuario usuario) {
        Trabajador trabajador = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUsuario(usuario.getIdUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.CONFLICT,
                                Map.of("detalle", "El trabajador no tiene datos laborales registrados."))),
                "Trabajador", "resolverContexto"
        );

        List<String> modulos = RepositoryExecutor.execute(
                        () -> rolModuloRepository.buscarPorTipoYNivel("trabajador", trabajador.getNivelAcceso()),
                        "RolModulo", "resolverContexto"
                ).stream()
                .map(rm -> rm.getModulo().getNombre())
                .toList();

        return ContextoSesionDTO.builder()
                .idNegocio(trabajador.getNegocio().getIdNegocio())
                .nivelAcceso(trabajador.getNivelAcceso())
                .modulos(modulos)
                .build();
    }
}
