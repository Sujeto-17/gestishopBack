package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AsignarHorarioRequestDTO;
import mx.com.gestishop.application.dto.response.TrabajadorHorarioResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.TrabajadorHorarioService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Horario de un trabajador: consultar su historial y cambiarlo
@Tag(name = "Horarios de trabajadores", description = "Asignación y historial de horarios por trabajador")
@RestController
@RequestMapping("/api/v1/trabajadores/{uuidUsuario}/horarios")
@RequiredArgsConstructor
public class TrabajadorHorarioController extends BaseController {

    private final TrabajadorHorarioService trabajadorHorarioService;
    private final SesionActual sesionActual;

    @Operation(summary = "Historial de horarios del trabajador",
            description = "Del más reciente al más antiguo; vigente=true marca el que rige hoy y programado=true los futuros.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('trabajadores')")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<TrabajadorHorarioResponseDTO>>> historial(@PathVariable UUID uuidUsuario) {
        return ok(trabajadorHorarioService.historial(uuidUsuario, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Cambiar el horario del trabajador",
            description = "Cierra el horario vigente el día anterior a vigenteDesde y abre el nuevo. El historial se conserva.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('trabajadores')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<TrabajadorHorarioResponseDTO>> asignar(
            @PathVariable UUID uuidUsuario, @Valid @RequestBody AsignarHorarioRequestDTO dto) {
        return okCreado(trabajadorHorarioService.asignar(uuidUsuario, dto, sesionActual.idNegocioObligatorio()));
    }
}
