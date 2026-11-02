package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

// Horario con sus días y con los datos que la pantalla necesita para decidir qué botones mostrar
@Getter
@Builder
@Schema(name = "HorarioResponseDTO", description = "Horario de trabajo con sus días")
public class HorarioResponseDTO {

    @Schema(description = "Identificador público")
    private UUID uuidHorario;

    private String nombre;

    @Schema(description = "activo | inactivo (cancelado)", example = "activo")
    private String estatus;

    @Schema(description = "Resumen legible para mostrar en listas y selectores", example = "L-V 08:00–16:00 · S 08:00–13:00")
    private String resumen;

    private List<HorarioDiaResponseDTO> dias;

    @Schema(description = "true si es un horario global del sistema (no se puede editar ni cancelar)")
    private boolean global;

    @Schema(description = "true si algún trabajador lo usó alguna vez (queda bloqueado: solo se puede cancelar)")
    private boolean usado;

    @Schema(description = "true si se puede editar o eliminar: es propio y nunca se ha usado")
    private boolean editable;

    @Schema(description = "Trabajadores que lo tienen vigente o programado; mientras sea mayor que 0 no se puede cancelar")
    private int trabajadoresVigentes;
}
