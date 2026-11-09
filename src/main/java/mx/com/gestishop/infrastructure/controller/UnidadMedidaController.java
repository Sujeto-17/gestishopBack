package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.UnidadMedidaResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.UnidadMedidaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Catálogo general de unidades de medida, para elegir la unidad de un producto
@Tag(name = "Unidades de medida", description = "Pieza, metro, kilogramo, litro...")
@RestController
@RequestMapping("/api/v1/unidades-medida")
@RequiredArgsConstructor
public class UnidadMedidaController extends BaseController {

    private final UnidadMedidaService unidadMedidaService;

    @Operation(summary = "Listar unidades de medida activas")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<UnidadMedidaResponseDTO>>> listar() {
        return ok(unidadMedidaService.listar());
    }
}

