package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Costo con el que el admin completa un resurtido que quedó pendiente
@Getter
@Setter
@Schema(name = "CostoMovimientoRequestDTO", description = "Costo unitario de un resurtido pendiente")
public class CostoMovimientoRequestDTO {

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Schema(example = "62.00")
    private BigDecimal costoUnitario;
}
