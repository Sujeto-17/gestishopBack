package mx.com.gestishop.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

// Un día trabajado dentro de un horario, con su entrada y salida
@Getter
@Setter
@Schema(name = "HorarioDiaRequestDTO", description = "Día de la semana con su hora de entrada y de salida")
public class HorarioDiaRequestDTO {

    @NotNull
    @Min(value = 1, message = "El día debe estar entre 1 (lunes) y 7 (domingo)")
    @Max(value = 7, message = "El día debe estar entre 1 (lunes) y 7 (domingo)")
    @Schema(description = "Día de la semana: 1 = lunes ... 7 = domingo", example = "1")
    private Integer diaSemana;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "08:00")
    private LocalTime horaEntrada;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", description = "Si es anterior a la entrada, el turno termina al día siguiente", example = "16:00")
    private LocalTime horaSalida;
}
