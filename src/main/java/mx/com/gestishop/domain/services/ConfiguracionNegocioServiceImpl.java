package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.ConfiguracionFiscalRequestDTO;
import mx.com.gestishop.application.dto.response.ConfiguracionNegocioResponseDTO;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.ConfiguracionNegocioService;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.repository.NegocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionNegocioServiceImpl implements ConfiguracionNegocioService {

    private final NegocioRepository negocioRepository;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionNegocioResponseDTO obtener(Long idNegocio) {
        return aResponse(buscar(idNegocio));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfiguracionNegocioResponseDTO actualizarFiscal(Long idNegocio, ConfiguracionFiscalRequestDTO dto) {
        Negocio negocio = buscar(idNegocio);
        negocio.setEmiteFactura(dto.getEmiteFactura());
        negocio.setPreciosIncluyenIva(dto.getPreciosIncluyenIva());

        Negocio guardado = RepositoryExecutor.execute(
                () -> negocioRepository.save(negocio), "Negocio", "actualizarConfiguracionFiscal");
        return aResponse(guardado);
    }

    // --- APOYO ---

    // El negocio siempre viene del token; aquí solo se carga
    private Negocio buscar(Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");
    }

    private ConfiguracionNegocioResponseDTO aResponse(Negocio negocio) {
        return ConfiguracionNegocioResponseDTO.builder()
                .nombreNegocio(negocio.getNombre())
                .emiteFactura(negocio.getEmiteFactura())
                .preciosIncluyenIva(negocio.getPreciosIncluyenIva())
                .build();
    }
}

