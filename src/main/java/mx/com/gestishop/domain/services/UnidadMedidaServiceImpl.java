package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.UnidadMedidaResponseDTO;
import mx.com.gestishop.application.mapper.CategoriaMapper;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.interfaces.UnidadMedidaService;
import mx.com.gestishop.domain.repository.CatUnidadMedidaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadMedidaServiceImpl implements UnidadMedidaService {

    private final CatUnidadMedidaRepository unidadRepository;

    @Override
    public List<UnidadMedidaResponseDTO> listar() {
        return RepositoryExecutor.execute(
                        () -> unidadRepository.findByEstatusOrderByOrdenAsc("activo"), "CatUnidadMedida", "listar")
                .stream().map(CategoriaMapper::toResponse).toList();
    }
}

