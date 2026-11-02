package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

// Asigna un horario a un trabajador a partir de una fecha (la tolerancia es del trabajador, no del horario)
@Getter
@Setter
@Schema(name = "AsignarHorarioRequestDTO", description = "Horario que tendrá un trabajador")
public class AsignarHorarioRequestDTO {

    @NotNull
    @Schema(description = "Identificador público del horario (global o del negocio)")
    private UUID uuidHorario;

    @Schema(description = "Desde cuándo rige. Vacío = hoy (o la fecha de ingreso al dar de alta al trabajador)", nullable = true)
    private LocalDate vigenteDesde;
}
