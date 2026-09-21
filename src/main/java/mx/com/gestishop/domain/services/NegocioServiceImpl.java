package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.NegocioRequestDTO;
import mx.com.gestishop.application.dto.response.NegocioResponseDTO;
import mx.com.gestishop.application.mapper.NegocioMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.NegocioService;
import mx.com.gestishop.domain.interfaces.SuscripcionService;
import mx.com.gestishop.domain.model.CatGiroNegocio;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.repository.CatGiroNegocioRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de la gestión de negocios. Es el último eslabón de la
 * cadena de dependencias: requiere que el giro y el plan ya existan en BD
 * (ambos son FK obligatorias), por eso este servicio se construye AL FINAL,
 * después de tener CatGiroNegocioService y PlanService funcionando.
 */
@Service
@RequiredArgsConstructor
public class NegocioServiceImpl implements NegocioService {

    private final NegocioRepository negocioRepository;
    private final CatGiroNegocioRepository catGiroNegocioRepository;
    private final PlanRepository planRepository;

    private final SuscripcionService suscripcionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO crear(NegocioRequestDTO dto) {
        // Validar correo único entre negocios activos
        boolean correoExiste = RepositoryExecutor.execute(
                () -> negocioRepository.existeCorreoActivo(dto.getCorreo()), "Negocio", "validarCorreo");

        if (correoExiste) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe un negocio registrado con ese correo."));
        }

        // Cargar el giro y el plan (ambos deben existir, son FK obligatorias)
        CatGiroNegocio giro = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.findById(dto.getIdGiro())
                        .orElseThrow(() -> ErrorFactory.notFound("CatGiroNegocio", dto.getIdGiro())),
                "CatGiroNegocio", "consultar"
        );

        Plan plan = RepositoryExecutor.execute(
                () -> planRepository.findById(dto.getIdPlan())
                        .orElseThrow(() -> ErrorFactory.notFound("Plan", dto.getIdPlan())),
                "Plan", "consultar"
        );

        // Construir y guardar la entidad (fechaAlta/vencimiento se calculan en el mapper)
        Negocio entidad = NegocioMapper.toEntity(dto);
        entidad.setGiro(giro);
        entidad.setPlan(plan);

        Negocio guardado = RepositoryExecutor.execute(() -> negocioRepository.save(entidad), "Negocio", "crear");

        // Se crea automáticamente el primer periodo de suscripción (prueba, estatus
        // 'pendiente'), con las mismas fechas que ya se calcularon para el negocio.
        // Al estar dentro de la misma transacción @Transactional del metodo, si algo
        // falla aquí, TAMBIÉN se revierte la creación del negocio (todo o nada)
        suscripcionService.crearPeriodoInicial(guardado, plan);

        return NegocioMapper.toResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO actualizar(UUID uuidNegocio, NegocioRequestDTO dto) {
        Negocio entidad = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "actualizar"
        );

        CatGiroNegocio giro = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.findById(dto.getIdGiro())
                        .orElseThrow(() -> ErrorFactory.notFound("CatGiroNegocio", dto.getIdGiro())),
                "CatGiroNegocio", "consultar"
        );

        entidad.setNombre(dto.getNombre());
        entidad.setLogoUrl(dto.getLogoUrl());
        entidad.setSistemaType(dto.getSistemaType());
        entidad.setGiro(giro);
        entidad.setCorreo(dto.getCorreo());
        entidad.setTelefono(dto.getTelefono());
        entidad.setDireccion(dto.getDireccion());

        Negocio actualizado = RepositoryExecutor.execute(() -> negocioRepository.save(entidad), "Negocio", "actualizar");
        return NegocioMapper.toResponse(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO cambiarEstatus(UUID uuidNegocio) {
        Negocio entidad = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "cambiarEstatus"
        );

        entidad.setEstatus("activo".equals(entidad.getEstatus()) ? "inactivo" : "activo");

        Negocio actualizado = RepositoryExecutor.execute(() -> negocioRepository.save(entidad), "Negocio", "cambiarEstatus");
        return NegocioMapper.toResponse(actualizado);
    }

    @Override
    public NegocioResponseDTO obtenerPorUuid(UUID uuidNegocio) {
        Negocio entidad = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "consultar"
        );
        return NegocioMapper.toResponse(entidad);
    }

    @Override
    public List<NegocioResponseDTO> listarActivos() {
        return RepositoryExecutor.execute(negocioRepository::listarActivos, "Negocio", "listarActivos")
                .stream().map(NegocioMapper::toResponse).toList();
    }
}
