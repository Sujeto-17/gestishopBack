package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.SuscripcionRequestDTO;
import mx.com.gestishop.application.dto.response.SuscripcionResponseDTO;
import mx.com.gestishop.application.mapper.SuscripcionMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.SuscripcionService;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.model.Suscripcion;
import mx.com.gestishop.domain.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Implementación del historial de suscripciones. Es el módulo que le da
 * sentido de negocio al campo negocios.vencimiento: cada renovación o
 * pago queda registrado como una fila propia, sin perder el historial.
 */
@Service
@RequiredArgsConstructor
public class SuscripcionServiceImpl implements SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Suscripcion crearPeriodoInicial(Negocio negocio, Plan plan) {
        Suscripcion periodo = SuscripcionMapper.crearPeriodoPrueba(negocio, plan);
        return RepositoryExecutor.execute(
                () -> suscripcionRepository.save(periodo), "Suscripcion", "crearPeriodoInicial");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SuscripcionResponseDTO confirmarPago(Long idSuscripcion, SuscripcionRequestDTO dto) {
        Suscripcion entidad = RepositoryExecutor.execute(
                () -> suscripcionRepository.findById(idSuscripcion)
                        .orElseThrow(() -> ErrorFactory.notFound("Suscripcion", idSuscripcion)),
                "Suscripcion", "confirmarPago"
        );

        if ("pagada".equals(entidad.getEstatus())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Este periodo ya fue marcado como pagado anteriormente."));
        }

        if ("cancelada".equals(entidad.getEstatus())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "No se puede pagar un periodo cancelado."));
        }

        entidad.setFechaPago(dto.getFechaPago());
        entidad.setMetodoPago(dto.getMetodoPago());
        entidad.setEstatus("pagada");
        if (dto.getNotas() != null) entidad.setNotas(dto.getNotas());

        Suscripcion actualizado = RepositoryExecutor.execute(
                () -> suscripcionRepository.save(entidad), "Suscripcion", "confirmarPago");

        return SuscripcionMapper.toResponse(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SuscripcionResponseDTO renovar(Long idNegocio) {
        Suscripcion vigente = RepositoryExecutor.execute(
                () -> suscripcionRepository.buscarPeriodoVigente(idNegocio)
                        .orElseThrow(() -> new ApiResponseException(ApiCodeResponse.RESOURCE_NOT_FOUND,
                                Map.of("detalle", "El negocio no tiene ningún periodo de suscripción registrado."))),
                "Suscripcion", "renovar"
        );

        // La fecha de inicio del nuevo periodo es la mayor entre hoy y el vencimiento
        // anterior: si renuevan antes de vencer, no se "pierden" los días restantes
        LocalDate hoy = LocalDate.now();
        LocalDate fechaInicio = vigente.getFechaVencimiento().isAfter(hoy)
                ? vigente.getFechaVencimiento()
                : hoy;

        Suscripcion nuevoPeriodo = SuscripcionMapper.crearRenovacion(
                vigente.getNegocio(), vigente.getPlan(), fechaInicio);

        Suscripcion guardado = RepositoryExecutor.execute(
                () -> suscripcionRepository.save(nuevoPeriodo), "Suscripcion", "renovar");

        return SuscripcionMapper.toResponse(guardado);
    }

    @Override
    public List<SuscripcionResponseDTO> listarPorNegocio(Long idNegocio) {
        return RepositoryExecutor.execute(
                        () -> suscripcionRepository.listarPorNegocio(idNegocio), "Suscripcion", "listarPorNegocio")
                .stream().map(SuscripcionMapper::toResponse).toList();
    }

    @Override
    public List<SuscripcionResponseDTO> listarVencidasPendientes() {
        return RepositoryExecutor.execute(
                        () -> suscripcionRepository.listarVencidasPendientes(LocalDate.now()),
                        "Suscripcion", "listarVencidasPendientes")
                .stream().map(SuscripcionMapper::toResponse).toList();
    }
}
