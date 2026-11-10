package mx.com.gestishop.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Variante nueva: sus valores (uno por atributo del producto), precios y existencia inicial
@Getter
@Setter
@Schema(name = "VarianteRequestDTO", description = "Variante a crear")
public class VarianteRequestDTO extends VarianteDatosRequestDTO {

    @Schema(description = "Un valor por cada atributo del producto; vacío en un producto sin atributos")
    private List<UUID> uuidsValores;

    @DecimalMin("0.000")
    @Digits(integer = 9, fraction = 3)
    @Schema(description = "Existencia con la que arranca; 0 o vacío = se registra sin stock. Se guarda como movimiento 'inventario_inicial'")
    private BigDecimal existenciaInicial;
}
