package mx.com.gestishop.infrastructure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.HorarioRequestDTO;
import mx.com.gestishop.application.dto.response.HorarioResponseDTO;
import mx.com.gestishop.application.dto.response.HorarioVigenteTrabajadorResponseDTO;
import mx.com.gestishop.application.dto.response.ToleranciaResponseDTO;
import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.generic.BaseController;
import mx.com.gestishop.domain.interfaces.HorarioService;
import mx.com.gestishop.domain.interfaces.TrabajadorHorarioService;
import mx.com.gestishop.infrastructure.config.SesionActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Horarios de trabajo del negocio. El negocio siempre sale del token, nunca de la petición.
@Tag(name = "Horarios", description = "Horarios de trabajo del personal (globales y propios del negocio)")
@RestController
@RequestMapping("/api/v1/horarios")
@RequiredArgsConstructor
public class HorarioController extends BaseController {

    private final HorarioService horarioService;
    private final TrabajadorHorarioService trabajadorHorarioService;
    private final SesionActual sesionActual;

    // Para elegir horario al dar de alta a un trabajador basta con el módulo de trabajadores
    @Operation(summary = "Listar horarios disponibles",
            description = "Globales + propios del negocio. Con soloActivos=true se omiten los cancelados (para selectores).")
    @PreAuthorize("hasRole('ADMIN') and (@sesionActual.tieneModulo('horarios') or @sesionActual.tieneModulo('trabajadores'))")
    @GetMapping
    public ResponseEntity<ApiDataResponseDTO<List<HorarioResponseDTO>>> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ok(horarioService.listar(sesionActual.idNegocioObligatorio(), soloActivos));
    }

    @Operation(summary = "Catálogo de tolerancias", description = "0, 5, 10, 15, 20, 25 y 30 minutos.")
    @PreAuthorize("hasRole('ADMIN') and (@sesionActual.tieneModulo('horarios') or @sesionActual.tieneModulo('trabajadores'))")
    @GetMapping("/tolerancias")
    public ResponseEntity<ApiDataResponseDTO<List<ToleranciaResponseDTO>>> tolerancias() {
        return ok(horarioService.listarTolerancias());
    }

    @Operation(summary = "Horario vigente de cada trabajador",
            description = "Una sola consulta para mostrar el horario de hoy en el listado de trabajadores.")
    @PreAuthorize("hasRole('ADMIN') and (@sesionActual.tieneModulo('horarios') or @sesionActual.tieneModulo('trabajadores'))")
    @GetMapping("/vigentes")
    public ResponseEntity<ApiDataResponseDTO<List<HorarioVigenteTrabajadorResponseDTO>>> vigentes() {
        return ok(trabajadorHorarioService.vigentesDelNegocio(sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Obtener un horario")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('horarios')")
    @GetMapping("/{uuidHorario}")
    public ResponseEntity<ApiDataResponseDTO<HorarioResponseDTO>> obtener(@PathVariable UUID uuidHorario) {
        return okEncontrado(horarioService.obtener(uuidHorario, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Crear un horario propio",
            description = "Rechaza nombres repetidos y horarios con los mismos días y horas que uno existente (incluidos los globales).")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('horarios')")
    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<HorarioResponseDTO>> crear(@Valid @RequestBody HorarioRequestDTO dto) {
        return okCreado(horarioService.crear(dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Editar un horario propio",
            description = "Solo si nadie lo ha usado. Los globales y los ya usados no se pueden modificar.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('horarios')")
    @PutMapping("/{uuidHorario}")
    public ResponseEntity<ApiDataResponseDTO<HorarioResponseDTO>> actualizar(
            @PathVariable UUID uuidHorario, @Valid @RequestBody HorarioRequestDTO dto) {
        return okActualizado(horarioService.actualizar(uuidHorario, dto, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Cancelar o reactivar un horario propio",
            description = "Cancelar solo es posible si ningún trabajador lo tiene vigente.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('horarios')")
    @PatchMapping("/{uuidHorario}/estatus")
    public ResponseEntity<ApiDataResponseDTO<HorarioResponseDTO>> cambiarEstatus(@PathVariable UUID uuidHorario) {
        return okActualizado(horarioService.cambiarEstatus(uuidHorario, sesionActual.idNegocioObligatorio()));
    }

    @Operation(summary = "Eliminar un horario propio", description = "Solo si nunca se ha usado; si ya se usó, se cancela.")
    @PreAuthorize("hasRole('ADMIN') and @sesionActual.tieneModulo('horarios')")
    @DeleteMapping("/{uuidHorario}")
    public ResponseEntity<ApiDataResponseDTO<Void>> eliminar(@PathVariable UUID uuidHorario) {
        horarioService.eliminar(uuidHorario, sesionActual.idNegocioObligatorio());
        return okEliminado();
    }
}
