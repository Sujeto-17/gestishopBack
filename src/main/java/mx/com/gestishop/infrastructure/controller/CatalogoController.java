package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.dto.CatalogoRefDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Catálogos", description = "Listas de apoyo para formularios")
@RestController
@RequestMapping("/api/v1/catalogos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class CatalogoController extends BaseController {

    private final CatalogoService catalogoService;

    @GetMapping("/generos")
    public ResponseEntity<ApiDataResponseDTO<List<CatalogoRefDTO>>> generos() {
        return ok(catalogoService.generos());
    }

    @GetMapping("/estados-civiles")
    public ResponseEntity<ApiDataResponseDTO<List<CatalogoRefDTO>>> estadosCiviles() {
        return ok(catalogoService.estadosCiviles());
    }

    @GetMapping("/municipios")
    public ResponseEntity<ApiDataResponseDTO<List<CatalogoRefDTO>>> municipios() {
        return ok(catalogoService.municipios());
    }
}
