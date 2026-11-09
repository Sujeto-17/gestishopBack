package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.ConfiguracionFiscalRequestDTO;
import mx.com.gestishop.application.dto.response.ConfiguracionNegocioResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.ConfiguracionNegocioService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Ajustes del negocio en el que trabaja el admin. El negocio siempre sale del token, nunca de la petición.
@Tag(name = "Configuración del negocio", description = "Facturación, IVA y ajustes generales")
@RestController
@RequestMapping("/api/v1/configuracion/negocio")
@RequiredArgsConstructor
public class ConfiguracionNegocioController extends BaseController {

    private final ConfiguracionNegocioService configuracionService;
    private final SesionActual sesionActual;

    @Operation(summary = "Ajustes del negocio actual")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('configuracion')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<ConfiguracionNegocioResponseDTO>> obtener() {
        return okEncontrado(configuracionService.obtener(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Guardar la configuración de facturación e IVA",
            description = "Define si el negocio emite factura y, si lo hace, si sus precios ya incluyen IVA.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('configuracion')")
    @PutMapping("/fiscal")
    public ResponseEntity<ApiDataResponseDTO<ConfiguracionNegocioResponseDTO>> actualizarFiscal(
            @Valid @RequestBody ConfiguracionFiscalRequestDTO dto) {
        return okActualizado(configuracionService.actualizarFiscal(sesionActual.idNegocioObligatorio(), dto));
    }
}
