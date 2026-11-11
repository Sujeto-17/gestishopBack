package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.FotoOrdenRequestDTO;
import mx.com.gestishop.application.dto.response.FotoProductoResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.ProductoFotoService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// Fotos de los productos: ADMIN las administra y el gerente solo las consulta. El negocio sale del token.
@Tag(name = "Fotos de productos", description = "Una foto principal y hasta 4 más por producto")
@RestController
@RequestMapping("/api/v1/productos/{uuidProducto}/fotos")
@RequiredArgsConstructor
public class ProductoFotoController extends BaseController {

    private final ProductoFotoService fotoService;
    private final SesionActual sesionActual;

    @Operation(summary = "Fotos del producto", description = "La principal primero y luego las demás por su orden.")
    @PreAuthorize("hasAnyRole('ADMIN','TRABAJADOR') and @sesionActual.tieneModulo('productos')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<FotoProductoResponseDTO>>> listar(@PathVariable UUID uuidProducto) {
        return ok(fotoService.listar(uuidProducto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Subir una foto",
            description = "JPG o PNG de hasta 5 MB; se reduce a 1200 px y se genera su miniatura. "
                    + "Máximo 5 por producto; la primera que se sube queda como principal.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiDataResponseDTO<List<FotoProductoResponseDTO>>> subir(
            @PathVariable UUID uuidProducto, @RequestParam("archivo") MultipartFile archivo) {
        return okCreado(fotoService.subir(uuidProducto, archivo, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Marcar una foto como principal")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PatchMapping("/{uuidFoto}/principal")
    public ResponseEntity<ApiDataResponseDTO<List<FotoProductoResponseDTO>>> marcarPrincipal(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidFoto) {
        return okActualizado(fotoService.marcarPrincipal(uuidProducto, uuidFoto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Cambiar el orden de las fotos", description = "La lista debe traer todas las fotos del producto.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @PutMapping("/orden")
    public ResponseEntity<ApiDataResponseDTO<List<FotoProductoResponseDTO>>> reordenar(
            @PathVariable UUID uuidProducto, @Valid @RequestBody FotoOrdenRequestDTO dto) {
        return okActualizado(fotoService.reordenar(uuidProducto, dto.getUuidsFotos(), sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Borrar una foto", description = "Si era la principal, pasa a serlo la siguiente.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('productos')")
    @DeleteMapping("/{uuidFoto}")
    public ResponseEntity<ApiDataResponseDTO<List<FotoProductoResponseDTO>>> eliminar(
            @PathVariable UUID uuidProducto, @PathVariable UUID uuidFoto) {
        return okActualizado(fotoService.eliminar(uuidProducto, uuidFoto, sesionActual.idNegocioObligatorio()));
    }
}
