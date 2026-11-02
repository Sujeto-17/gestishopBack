package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// Horario que rige hoy para un trabajador; sirve para mostrarlo en el listado sin una consulta por trabajador
@Getter
@Builder
@Schema(name = "HorarioVigenteTrabajadorResponseDTO", description = "Horario vigente hoy de un trabajador")
public class HorarioVigenteTrabajadorResponseDTO {

    @Schema(description = "Identificador público del usuario del trabajador")
    private UUID uuidUsuario;

    private UUID uuidHorario;
    private String nombreHorario;

    @Schema(example = "L-V 08:00–16:00")
    private String resumen;
}