package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.CatModuloRequestDTO;
import mx.com.gestishop.application.dto.response.ModuloResponseDTO;
import mx.com.gestishop.application.mapper.CatModuloMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.CatModuloService;
import mx.com.gestishop.domain.model.CatModulo;
import mx.com.gestishop.domain.repository.CatModuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Implementación del CRUD de módulos del sistema
@Service
@RequiredArgsConstructor
public class CatModuloServiceImpl implements CatModuloService {

    private final CatModuloRepository catModuloRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuloResponseDTO crear(CatModuloRequestDTO dto) {
        boolean existe = RepositoryExecutor.execute(
                () -> catModuloRepository.existeNombre(dto.getNombre()), "CatModulo", "validarNombre");

        if (existe) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe un módulo registrado con ese nombre."));
        }

        // La clave viene en el DTO o se genera del nombre; debe ser única porque es lo que usa el código
        CatModulo nuevo = CatModuloMapper.toEntity(dto);

        boolean claveExiste = RepositoryExecutor.execute(
                () -> catModuloRepository.existeClave(nuevo.getClave()), "CatModulo", "validarClave");

        if (claveExiste) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe un módulo con la clave «" + nuevo.getClave() + "». Indica otra clave."));
        }

        CatModulo guardado = RepositoryExecutor.execute(
                () -> catModuloRepository.save(nuevo), "CatModulo", "crear");

        return CatModuloMapper.toResponse(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuloResponseDTO actualizar(Long idModulo, CatModuloRequestDTO dto) {
        CatModulo entidad = RepositoryExecutor.execute(
                () -> catModuloRepository.findById(idModulo).orElseThrow(() -> ErrorFactory.notFound("CatModulo", idModulo)),
                "CatModulo", "actualizar"
        );

        // Al renombrar, el nuevo nombre no debe estar en uso por otro módulo (la clave NO cambia)
        boolean nombreEnUso = RepositoryExecutor.execute(
                () -> catModuloRepository.existeNombreEnOtro(idModulo, dto.getNombre()), "CatModulo", "validarNombre");

        if (nombreEnUso) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe otro módulo con ese nombre."));
        }

        CatModuloMapper.actualizarEntidad(entidad, dto);

        return CatModuloMapper.toResponse(
                RepositoryExecutor.execute(() -> catModuloRepository.save(entidad), "CatModulo", "actualizar"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuloResponseDTO cambiarEstatus(Long idModulo) {
        CatModulo entidad = RepositoryExecutor.execute(
                () -> catModuloRepository.findById(idModulo).orElseThrow(() -> ErrorFactory.notFound("CatModulo", idModulo)),
                "CatModulo", "cambiarEstatus"
        );

        entidad.setEstatus("activo".equals(entidad.getEstatus()) ? "inactivo" : "activo");

        return CatModuloMapper.toResponse(
                RepositoryExecutor.execute(() -> catModuloRepository.save(entidad), "CatModulo", "cambiarEstatus"));
    }

    @Override
    public List<ModuloResponseDTO> listarTodos() {
        return RepositoryExecutor.execute(catModuloRepository::listarTodos, "CatModulo", "listarTodos")
                .stream().map(CatModuloMapper::toResponse).toList();
    }

    @Override
    public List<ModuloResponseDTO> listarActivos() {
        return RepositoryExecutor.execute(catModuloRepository::listarActivos, "CatModulo", "listarActivos")
                .stream().map(CatModuloMapper::toResponse).toList();
    }
}
