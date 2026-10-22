package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.repository.AdminNegocioRepository;
import mx.com.gestishop.domain.repository.TrabajadorRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Valida los límites que define el plan de un negocio (campos max_* de la tabla planes; null = sin límite).
 * <p>
 * Un solo lugar para todos los módulos: cuando un módulo nuevo tenga un tope (por ejemplo productos), se agrega
 * aquí un método validarAgregar...() y se llama antes de crear. Se valida SOLO al agregar: un negocio que baja
 * a un plan más chico y ya excede el límite no se bloquea, simplemente no puede agregar más hasta quedar debajo.
 */
@Component
@RequiredArgsConstructor
public class PlanLimites {

    private final TrabajadorRepository trabajadorRepository;
    private final AdminNegocioRepository adminNegocioRepository;

    // Antes de registrar (o reactivar) un trabajador: ¿cabe uno más en el plan del negocio?
    public void validarAgregarTrabajador(Negocio negocio) {
        Plan plan = negocio.getPlan();
        Integer maximo = plan.getMaxTrabajadores();
        if (maximo == null) return;

        long actuales = RepositoryExecutor.execute(
                () -> trabajadorRepository.contarActivosPorNegocio(negocio.getIdNegocio()),
                "Trabajador", "contarActivos");

        validar(actuales, maximo, plan, "trabajadores activos");
    }

    // Antes de registrar, asignar o reactivar un administrador: ¿cabe uno más en el plan del negocio?
    public void validarAgregarAdministrador(Negocio negocio) {
        Plan plan = negocio.getPlan();
        Integer maximo = plan.getMaxAdministradores();
        if (maximo == null) return;

        long actuales = RepositoryExecutor.execute(
                () -> adminNegocioRepository.contarAdminsActivos(negocio.getIdNegocio()),
                "AdminNegocio", "contarActivos");

        validar(actuales, maximo, plan, "administradores activos");
    }

    private void validar(long actuales, int maximo, Plan plan, String recurso) {
        if (actuales >= maximo) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "El plan " + plan.getNombre() + " permite hasta " + maximo + " " + recurso
                            + " y este negocio ya tiene " + actuales + ". Cambia el plan del negocio o desactiva a alguien."));
        }
    }
}
