package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AtributoValorRequestDTO;
import mx.com.gestishop.application.dto.response.AtributoResponseDTO;
import mx.com.gestishop.application.dto.response.AtributoValorResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.AtributoService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Atributos para variantes, vistos por el negocio. El negocio siempre sale del token.
@Tag(name = "Atributos", description = "Atributos de variante (Color, Talla...) y valores propios del negocio")
@RestController
@RequestMapping("/api/v1/atributos")
@RequiredArgsConstructor
public class AtributoController extends BaseController {

    private final AtributoService atributoService;
    private final SesionActual sesionActual;

    @Operation(summary = "Listar atributos con sus valores",
            description = "Atributos activos con valores del sistema y propios del negocio (campo propio=true).")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<AtributoResponseDTO>>> listar() {
        return ok(atributoService.listarParaNegocio(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Agregar un valor propio a un atributo",
            description = "No permite repetir un valor del sistema ni uno ya registrado por el negocio.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PostMapping("/{uuidAtributo}/valores")
    public ResponseEntity<ApiDataResponseDTO<AtributoValorResponseDTO>> crearValor(
            @PathVariable UUID uuidAtributo, @Valid @RequestBody AtributoValorRequestDTO dto) {
        return okCreado(atributoService.crearValorPropio(uuidAtributo, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Renombrar un valor propio")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PutMapping("/valores/{uuidValor}")
    public ResponseEntity<ApiDataResponseDTO<AtributoValorResponseDTO>> actualizarValor(
            @PathVariable UUID uuidValor, @Valid @RequestBody AtributoValorRequestDTO dto) {
        return okActualizado(atributoService.actualizarValorPropio(uuidValor, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Eliminar un valor propio",
            description = "Si alguna variante lo usa, se desactiva en lugar de eliminarse.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @DeleteMapping("/valores/{uuidValor}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminarValor(@PathVariable UUID uuidValor) {
        atributoService.eliminarValorPropio(uuidValor, sesionActual.idNegocioObligatorio());
        return okEliminado();
    }
}
