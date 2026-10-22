package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.CatModuloRequestDTO;
import mx.com.gestishop.application.dto.response.ModuloResponseDTO;
import mx.com.gestishop.domain.model.CatModulo;

import java.text.Normalizer;
import java.util.Locale;

// Convierte entre la entidad CatModulo y sus DTOs de entrada/salida
public class CatModuloMapper {

    private CatModuloMapper() {
    }

    public static CatModulo toEntity(CatModuloRequestDTO dto) {
        return CatModulo.builder()
                .clave(dto.getClave() != null ? dto.getClave() : generarClave(dto.getNombre()))
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .icono(dto.getIcono())
                .ruta(dto.getRuta())
                .orden(dto.getOrden() != null ? dto.getOrden() : 0)
                .estatus("activo")
                .build();
    }

    public static void actualizarEntidad(CatModulo entidad, CatModuloRequestDTO dto) {
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setIcono(dto.getIcono());
        entidad.setRuta(dto.getRuta());
        if (dto.getOrden() != null) entidad.setOrden(dto.getOrden());
    }

    public static ModuloResponseDTO toResponse(CatModulo entidad) {
        return ModuloResponseDTO.builder()
                .idModulo(entidad.getIdModulo())
                .clave(entidad.getClave())
                .nombre(entidad.getNombre())
                .descripcion(entidad.getDescripcion())
                .icono(entidad.getIcono())
                .ruta(entidad.getRuta())
                .orden(entidad.getOrden())
                .estatus(entidad.getEstatus())
                .build();
    }

    /**
     * Genera la clave de un módulo a partir de su nombre: "Categorías y marcas" -> "categorias_y_marcas".
     * Se usa solo al CREAR si no se envió una clave; después la clave ya no cambia.
     */
    public static String generarClave(String nombre) {
        String sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String clave = sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (clave.isEmpty() || !Character.isLetter(clave.charAt(0))) clave = "m_" + clave;
        return clave.length() > 40 ? clave.substring(0, 40) : clave;
    }
}
