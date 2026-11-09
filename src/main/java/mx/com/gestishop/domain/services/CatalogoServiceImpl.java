package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.RegimenFiscalResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.domain.interfaces.CatalogoService;
import mx.com.gestishop.domain.repository.CatEstadoCivilRepository;
import mx.com.gestishop.domain.repository.CatGeneroRepository;
import mx.com.gestishop.domain.repository.CatMunicipioRepository;
import mx.com.gestishop.domain.repository.CatRegimenFiscalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CatGeneroRepository generoRepository;
    private final CatEstadoCivilRepository estadoCivilRepository;
    private final CatMunicipioRepository municipioRepository;
    private final CatRegimenFiscalRepository regimenFiscalRepository;

    @Override
    public List<CatalogoRefDTO> generos() {
        return RepositoryExecutor.execute(() -> generoRepository.findAll(Sort.by("nombre")), "CatGenero", "listar")
                .stream().map(g -> ref(g.getIdGenero(), g.getNombre())).toList();
    }

    @Override
    public List<CatalogoRefDTO> estadosCiviles() {
        return RepositoryExecutor.execute(() -> estadoCivilRepository.findAll(Sort.by("nombre")), "CatEstadoCivil", "listar")
                .stream().map(e -> ref(e.getIdEstadoCivil(), e.getNombre())).toList();
    }

    @Override
    public List<CatalogoRefDTO> municipios() {
        return RepositoryExecutor.execute(() -> municipioRepository.findAll(Sort.by("nombre")), "CatMunicipio", "listar")
                .stream().map(m -> ref(m.getIdMunicipio(), m.getNombre())).toList();
    }

    @Override
    public List<RegimenFiscalResponseDTO> regimenesFiscales() {
        return RepositoryExecutor.execute(
                        () -> regimenFiscalRepository.findByEstatusOrderByClaveAsc("activo"), "CatRegimenFiscal", "listar")
                .stream()
                .map(r -> RegimenFiscalResponseDTO.builder()
                        .clave(r.getClave())
                        .descripcion(r.getDescripcion())
                        .personaFisica(r.getPersonaFisica())
                        .personaMoral(r.getPersonaMoral())
                        .build())
                .toList();
    }

    private static CatalogoRefDTO ref(Long id, String nombre) {
        return CatalogoRefDTO.builder().id(id.intValue()).nombre(nombre).build();
    }
}
