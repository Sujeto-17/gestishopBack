package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.ConfiguracionFiscalRequestDTO;
import mx.com.gestishop.application.dto.response.ConfiguracionNegocioResponseDTO;

// Ajustes que el admin controla del negocio en el que está trabajando
public interface ConfiguracionNegocioService {

    // Ajustes actuales del negocio
    ConfiguracionNegocioResponseDTO obtener(Long idNegocio);

    // Guarda la configuración de facturación e IVA
    ConfiguracionNegocioResponseDTO actualizarFiscal(Long idNegocio, ConfiguracionFiscalRequestDTO dto);
}
