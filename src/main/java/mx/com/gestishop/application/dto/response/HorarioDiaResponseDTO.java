package mx.com.gestishop.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

// Un día de un horario
@Getter
@Builder
@Schema(name = "HorarioDiaResponseDTO", description = "Día de la semana con su entrada y salida")
public class HorarioDiaResponseDTO {

    @Schema(description = "1 = lunes ... 7 = domingo")
    private Integer diaSemana;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "08:00")
    private LocalTime horaEntrada;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "16:00")
    private LocalTime horaSalida;

    @Schema(description = "true si la salida es al día siguiente (turno nocturno)")
    private boolean cruzaMedianoche;
}