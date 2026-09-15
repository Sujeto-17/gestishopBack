package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.RegistrarUsuarioRequestDTO;
import mx.com.gestishop.application.dto.response.SesionResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestión de usuarios. El registro NO es público:
 * solo el superadmin puede dar de alta nuevos admins (y a su vez, cada
 * admin da de alta a sus propios trabajadores desde el módulo de Trabajadores,
 * no desde aquí). El primer superadmin del sistema se crea por script de
 * seed directamente en base de datos, jamás vía endpoint público, para
 * evitar que cualquiera se autoasigne el rol más alto del sistema.
 */
@Tag(name = "Usuarios", description = "Alta y gestión de cuentas de acceso")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController extends BaseController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Registrar un nuevo administrador",
            description = "Solo accesible por superadmin. Crea la cuenta y la vincula al negocio indicado.")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<SesionResponseDTO>> registrar(
            @Valid @RequestBody RegistrarUsuarioRequestDTO dto) {
        return okCreado(usuarioService.registrar(dto));
    }
}
