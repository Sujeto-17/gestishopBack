package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CatGiroNegocioRequestDTO;
import mx.com.gestishop.application.dto.response.CatGiroNegocioResponseDTO;
import mx.com.gestishop.application.mapper.CatGiroNegocioMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.CatGiroNegocioService;
import mx.com.gestishop.domain.model.CatGiroNegocio;
import mx.com.gestishop.domain.repository.CatGiroNegocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Implementación del CRUD de giros de negocio. Es un catálogo simple, sin relaciones complejas
@Service
@RequiredArgsConstructor
public class CatGiroNegocioServiceImpl implements CatGiroNegocioService {

    private final CatGiroNegocioRepository catGiroNegocioRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatGiroNegocioResponseDTO crear(CatGiroNegocioRequestDTO dto) {
        boolean existe = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.existeNombre(dto.getNombre()),
                "CatGiroNegocio", "validarNombre"
        );

        if (existe) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe un giro registrado con ese nombre."));
        }

        CatGiroNegocio entidad = CatGiroNegocioMapper.toEntity(dto);
        CatGiroNegocio guardado = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.save(entidad),
                "CatGiroNegocio", "crear"
        );

        return CatGiroNegocioMapper.toResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatGiroNegocioResponseDTO actualizar(Long idGiro, CatGiroNegocioRequestDTO dto) {
        CatGiroNegocio entidad = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.findById(idGiro)
                        .orElseThrow(() -> ErrorFactory.notFound("CatGiroNegocio", idGiro)),
                "CatGiroNegocio", "actualizar"
        );

        CatGiroNegocioMapper.actualizarEntidad(entidad, dto);

        CatGiroNegocio actualizado = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.save(entidad),
                "CatGiroNegocio", "actualizar"
        );

        return CatGiroNegocioMapper.toResponse(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CatGiroNegocioResponseDTO cambiarEstatus(Long idGiro) {
        CatGiroNegocio entidad = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.findById(idGiro)
                        .orElseThrow(() -> ErrorFactory.notFound("CatGiroNegocio", idGiro)),
                "CatGiroNegocio", "cambiarEstatus"
        );

        entidad.setEstatus("activo".equals(entidad.getEstatus()) ? "inactivo" : "activo");

        CatGiroNegocio actualizado = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.save(entidad),
                "CatGiroNegocio", "cambiarEstatus"
        );

        return CatGiroNegocioMapper.toResponse(actualizado);
    }

    @Override
    public List<CatGiroNegocioResponseDTO> listarTodos() {
        return RepositoryExecutor.execute(catGiroNegocioRepository::listarTodos, "CatGiroNegocio", "listarTodos")
                .stream().map(CatGiroNegocioMapper::toResponse).toList();
    }

    @Override
    public List<CatGiroNegocioResponseDTO> listarActivos() {
        return RepositoryExecutor.execute(catGiroNegocioRepository::listarActivos, "CatGiroNegocio", "listarActivos")
                .stream().map(CatGiroNegocioMapper::toResponse).toList();
    }
}
