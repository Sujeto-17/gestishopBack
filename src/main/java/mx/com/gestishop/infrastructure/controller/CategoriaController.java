package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AdoptarCategoriasRequestDTO;
import mx.com.gestishop.application.dto.request.CategoriaRequestDTO;
import mx.com.gestishop.application.dto.response.CategoriaResponseDTO;
import mx.com.gestishop.application.dto.response.CategoriaSugeridaResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.CategoriaService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Categorías de productos del negocio. El negocio siempre sale del token, nunca de la petición.
@Tag(name = "Categorías", description = "Familias y subfamilias de productos del negocio")
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController extends BaseController {

    private final CategoriaService categoriaService;
    private final SesionActual sesionActual;

    // Para elegir categoría al dar de alta un producto basta con el módulo de productos
    @Operation(summary = "Listar categorías del negocio",
            description = "Familias y subfamilias en una sola lista (el front arma el árbol con uuidPadre). "
                    + "Con soloActivas=true se omiten las inactivas (para selectores).")
    @PreAuthorize("hasRole('ADMIN') and (@sesionActual.tieneModulo('categorias') or @sesionActual.tieneModulo('productos'))")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<CategoriaResponseDTO>>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        return ok(categoriaService.listar(sesionActual.idNegocioObligatorio(), soloActivas));
    }

    @Operation(summary = "Categorías sugeridas según el giro del negocio",
            description = "Indica cuáles ya existen en el negocio para no ofrecerlas de nuevo.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @GetMapping("/sugeridas")
    public ResponseEntity<ApiDataResponseDTO<List<CategoriaSugeridaResponseDTO>>> sugeridas() {
        return ok(categoriaService.listarSugeridas(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Copiar categorías sugeridas al negocio",
            description = "Crea las elegidas (y las familias que necesiten). Omite las que ya existen. Devuelve las creadas.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @PostMapping("/sugeridas/adoptar")
    public ResponseEntity<ApiDataResponseDTO<List<CategoriaResponseDTO>>> adoptar(
            @Valid @RequestBody AdoptarCategoriasRequestDTO dto) {
        return okCreado(categoriaService.adoptarSugeridas(dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Obtener una categoría")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @GetMapping("/{uuidCategoria}")
    public ResponseEntity<ApiDataResponseDTO<CategoriaResponseDTO>> obtener(@PathVariable UUID uuidCategoria) {
        return okEncontrado(categoriaService.obtener(uuidCategoria, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Crear una categoría",
            description = "Sin uuidPadre crea una familia; con uuidPadre, una subfamilia (máximo dos niveles).")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<CategoriaResponseDTO>> crear(@Valid @RequestBody CategoriaRequestDTO dto) {
        return okCreado(categoriaService.crear(dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Editar una categoría",
            description = "Permite renombrarla y moverla a otra familia. Una familia con subfamilias no puede volverse subfamilia.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @PutMapping("/{uuidCategoria}")
    public ResponseEntity<ApiDataResponseDTO<CategoriaResponseDTO>> actualizar(
            @PathVariable UUID uuidCategoria, @Valid @RequestBody CategoriaRequestDTO dto) {
        return okActualizado(categoriaService.actualizar(uuidCategoria, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Activar o desactivar una categoría",
            description = "Desactivar una familia desactiva sus subfamilias. Reactivar una subfamilia exige su familia activa.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @PatchMapping("/{uuidCategoria}/estatus")
    public ResponseEntity<ApiDataResponseDTO<CategoriaResponseDTO>> cambiarEstatus(@PathVariable UUID uuidCategoria) {
        return okActualizado(categoriaService.cambiarEstatus(uuidCategoria, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Eliminar una categoría",
            description = "Solo si no tiene subfamilias ni productos; si los tiene, se desactiva.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('categorias')")
    @DeleteMapping("/{uuidCategoria}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminar(@PathVariable UUID uuidCategoria) {
        categoriaService.eliminar(uuidCategoria, sesionActual.idNegocioObligatorio());
        return okEliminado();
    }
}
