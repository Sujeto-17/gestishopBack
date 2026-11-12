package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

// Corrección por conteo físico: se indica cuánto hay realmente y el sistema calcula la diferencia
@Getter
@Setter
@Schema(name = "AjusteInventarioRequestDTO", description = "Ajuste de existencia por conteo físico (solo admin)")
public class AjusteInventarioRequestDTO {

    @NotNull
    private UUID uuidVariante;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 9, fraction = 3)
    @Schema(description = "Existencia real contada", example = "38")
    private BigDecimal existenciaReal;

    @NotBlank(message = "Explica el motivo del ajuste")
    @Size(max = 300)
    private String notas;

    public void setNotas(String notas) {
        this.notas = notas == null ? null : notas.trim();
    }
}

