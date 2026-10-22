package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import mx.com.gestishop.application.dto.TrabajadorBaseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Schema(name = "TrabajadorActualizarRequestDTO", description = "Actualización de datos de un trabajador (sin contraseña)")
public class TrabajadorActualizarRequestDTO extends TrabajadorBaseDTO {
}
