package mx.com.gestishop.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "ProveedorContactoResponseDTO")
public class ProveedorContactoResponseDTO {

    private String nombre;
    private String puesto;
    private String telefono;
    private String whatsapp;
    private String correo;
    private boolean esPrincipal;
    private String notas;
}
