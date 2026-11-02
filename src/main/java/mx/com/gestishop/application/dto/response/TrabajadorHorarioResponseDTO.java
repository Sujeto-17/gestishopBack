package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Un renglón del historial de horarios de un trabajador
@Getter
@Builder
@Schema(name = "TrabajadorHorarioResponseDTO", description = "Horario que tuvo, tiene o tendrá un trabajador")
public class TrabajadorHorarioResponseDTO {

    private UUID uuidHorario;
    private String nombreHorario;

    @Schema(example = "L-V 08:00–16:00")
    private String resumen;

    private List<HorarioDiaResponseDTO> dias;

    private LocalDate vigenteDesde;

    @Schema(description = "null = sigue vigente", nullable = true)
    private LocalDate vigenteHasta;

    @Schema(description = "true si rige hoy")
    private boolean vigente;

    @Schema(description = "true si empieza en una fecha futura")
    private boolean programado;
}
