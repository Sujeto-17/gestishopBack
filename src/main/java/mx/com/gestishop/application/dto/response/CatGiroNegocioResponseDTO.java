package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

// Representación completa de un giro de negocio, usada en el CRUD del superadmin
@Getter
@Builder
@Schema(name = "CatGiroNegocioResponseDTO", description = "Giro de negocio con todos sus datos")
public class CatGiroNegocioResponseDTO {

    @Schema(description = "Identificador del giro")
    private Long idGiro;

    @Schema(description = "Nombre visible", example = "Restaurantes / comida")
    private String nombre;

    @Schema(description = "Ícono FontAwesome", example = "utensils", nullable = true)
    private String icono;

    @Schema(description = "Orden de aparición")
    private Short orden;

    @Schema(description = "Estatus del giro", example = "activo")
    private String estatus;
}
