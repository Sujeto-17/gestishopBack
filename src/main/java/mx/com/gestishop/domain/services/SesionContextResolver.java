package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.ContextoSesionDTO;
import mx.com.gestishop.application.dto.response.NegocioSesionDTO;
import mx.com.gestishop.application.mapper.NegocioMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.model.*;
import mx.com.gestishop.domain.repository.AdminNegocioRepository;
import mx.com.gestishop.domain.repository.RolModuloRepository;
import mx.com.gestishop.domain.repository.TrabajadorRepository;
import mx.com.gestishop.infrastructure.utils.FechaNegocioUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resuelve el contexto de sesión de un usuario ya autenticado: a qué negocio entra, su nivel de
 * acceso y qué módulos puede ver (siempre como CLAVES de módulo, no como nombres).
 * <p>
 * Se usa en el login, al renovar la sesión con un refresh token y al cambiar de negocio, para no
 * repetir el mismo switch por tipo de usuario en tres lugares. Cualquier cambio en las reglas de
 * permisos se hace UNA sola vez, aquí.
 */
@Component
@RequiredArgsConstructor
public class SesionContextResolver {

    // Formato con el que se muestra la fecha de vencimiento en los mensajes
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AdminNegocioRepository adminNegocioRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final RolModuloRepository rolModuloRepository;

    /** Contexto sin negocio preferido (login normal): un admin entra a su primer negocio disponible. */
    public ContextoSesionDTO resolver(Usuario usuario) {
        return resolver(usuario, null);
    }

    /**
     * Determina el contexto según el tipo de usuario:
     * - superadmin: sin negocio, sin restricción de módulos (su panel muestra todo).
     * - admin: puede tener VARIOS negocios (dueño con sucursales). Entra al negocio preferido (el que
     *   tenía seleccionado la sesión) si sigue disponible; si no, al primero disponible. Ve TODOS los
     *   módulos del plan de ese negocio.
     * - trabajador: pertenece a UN negocio, ve los módulos de su nivel de acceso QUE ADEMÁS estén
     *   incluidos en el plan de su negocio.
     *
     * @param idNegocioPreferido solo aplica a administradores (null = el primero disponible)
     */
    public ContextoSesionDTO resolver(Usuario usuario, Long idNegocioPreferido) {
        return switch (usuario.getTipoUsuario()) {
            case "admin" -> resolverAdmin(usuario, idNegocioPreferido, false);
            case "trabajador" -> resolverTrabajador(usuario);
            default -> ContextoSesionDTO.builder()
                    .idNegocio(null)
                    .nivelAcceso(null)
                    .modulos(List.of())
                    .negocios(List.of())
                    .build();
        };
    }

    /**
     * Igual que resolver() para un admin, pero ESTRICTO: el negocio pedido debe ser suyo y estar
     * disponible, de lo contrario falla (no cae silenciosamente a otro negocio). Se usa al cambiar
     * de negocio a petición del usuario.
     */
    public ContextoSesionDTO resolverEstricto(Usuario usuario, Long idNegocio) {
        return resolverAdmin(usuario, idNegocio, true);
    }

    private ContextoSesionDTO resolverAdmin(Usuario usuario, Long idPreferido, boolean estricto) {
        List<AdminNegocio> relaciones = RepositoryExecutor.execute(
                () -> adminNegocioRepository.buscarPorUsuarioConNegocio(usuario.getIdUsuario()),
                "AdminNegocio", "resolverContexto"
        );

        if (relaciones.isEmpty()) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "El usuario admin no tiene negocio asignado."));
        }

        List<Negocio> negocios = relaciones.stream().map(AdminNegocio::getNegocio).toList();

        Negocio elegido;
        if (estricto) {
            // El negocio pedido debe ser de este admin y estar vigente
            elegido = negocios.stream()
                    .filter(n -> n.getIdNegocio().equals(idPreferido))
                    .findFirst()
                    .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.FORBIDDEN,
                            Map.of("detalle", "No tienes acceso a ese negocio.")));
            validarNegocioVigente(elegido);
        } else {
            List<Negocio> disponibles = negocios.stream()
                    .filter(n -> motivoNoDisponible(n).isEmpty())
                    .toList();

            if (disponibles.isEmpty()) {
                // Con un solo negocio se explica su motivo exacto; con varios, un mensaje general
                String detalle = negocios.size() == 1
                        ? motivoNoDisponible(negocios.get(0)).orElse("El negocio no está disponible.")
                        : "Ninguno de tus negocios tiene acceso activo. Contacta a soporte.";
                throw new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
            }

            // El preferido si sigue disponible; si no (venció, lo desvincularon...), el primero disponible
            elegido = disponibles.stream()
                    .filter(n -> n.getIdNegocio().equals(idPreferido))
                    .findFirst()
                    .orElse(disponibles.get(0));
        }

        return ContextoSesionDTO.builder()
                .idNegocio(elegido.getIdNegocio())
                .nombreNegocio(elegido.getNombre())
                .nivelAcceso(null)  // admin no tiene nivel_acceso, ese campo es exclusivo de trabajador
                .modulos(List.copyOf(clavesActivas(elegido.getPlan().getModulos())))
                .negocios(negocios.stream().map(this::aNegocioSesion).toList())
                .build();
    }

    private ContextoSesionDTO resolverTrabajador(Usuario usuario) {
        Trabajador trabajador = RepositoryExecutor.execute(
                () -> trabajadorRepository.buscarPorUsuario(usuario.getIdUsuario())
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.CONFLICT,
                                Map.of("detalle", "El trabajador no tiene datos laborales registrados."))),
                "Trabajador", "resolverContexto"
        );

        Negocio negocio = trabajador.getNegocio();
        validarNegocioVigente(negocio);

        // Módulos que permite SU NIVEL (tabla rol_modulos)...
        List<CatModulo> delNivel = RepositoryExecutor.execute(
                        () -> rolModuloRepository.buscarPorTipoYNivel("trabajador", trabajador.getNivelAcceso()),
                        "RolModulo", "resolverContexto"
                ).stream()
                .map(rm -> rm.getModulo())
                .toList();

        // ...pero SOLO los que el plan del negocio incluye
        Set<String> delPlan = clavesActivas(negocio.getPlan().getModulos());
        List<String> modulos = clavesActivas(delNivel).stream()
                .filter(delPlan::contains)
                .toList();

        return ContextoSesionDTO.builder()
                .idNegocio(negocio.getIdNegocio())
                .nombreNegocio(negocio.getNombre())
                .nivelAcceso(trabajador.getNivelAcceso())
                .modulos(modulos)
                .negocios(List.of())
                .build();
    }

    // Convierte un negocio al renglón del selector, indicando si se puede usar y, si no, por qué
    private NegocioSesionDTO aNegocioSesion(Negocio negocio) {
        Optional<String> motivo = motivoNoDisponible(negocio);
        return NegocioSesionDTO.builder()
                .idNegocio(negocio.getIdNegocio())
                .nombre(negocio.getNombre())
                .logoUrl(NegocioMapper.construirUrlPublica(negocio.getLogoUrl()))
                .disponible(motivo.isEmpty())
                .motivo(motivo.orElse(null))
                .build();
    }

    /**
     * Un negocio NO se puede usar si su periodo (de prueba o pagado) ya terminó, o si el superadmin
     * lo suspendió a mano ('inactivo'). El corte se hace por FECHA (negocio.vencimiento) y no solo por
     * estatus, para que sea exacto aunque el proceso nocturno aún no haya corrido.
     * Para volver a entrar, el superadmin registra el pago del negocio en el módulo Suscripciones.
     *
     * @return el motivo en texto, o vacío si el negocio sí está disponible
     */
    private Optional<String> motivoNoDisponible(Negocio negocio) {
        LocalDate vencimiento = negocio.getVencimiento();

        if (vencimiento != null && vencimiento.isBefore(FechaNegocioUtil.hoy())) {
            return Optional.of("El periodo de acceso de este negocio terminó el "
                    + vencimiento.format(FORMATO_FECHA) + ". Contacta a soporte para reactivarlo.");
        }

        if ("inactivo".equals(negocio.getEstatus())) {
            return Optional.of("El negocio está suspendido. Contacta a soporte para reactivarlo.");
        }

        return Optional.empty();
    }

    // Versión que lanza el error cuando el negocio no está disponible (login de trabajadores, cambio de negocio)
    private void validarNegocioVigente(Negocio negocio) {
        motivoNoDisponible(negocio).ifPresent(motivo -> {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", motivo));
        });
    }

    // CLAVES de los módulos que están ACTIVOS en el catálogo (un módulo desactivado ya no se ofrece)
    private Set<String> clavesActivas(Collection<CatModulo> modulos) {
        return modulos.stream()
                .filter(m -> "activo".equals(m.getEstatus()))
                .map(CatModulo::getClave)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
