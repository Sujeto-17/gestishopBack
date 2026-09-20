package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.CatModuloRequestDTO;
import mx.com.gestishop.application.dto.response.ModuloResponseDTO;
import mx.com.gestishop.domain.model.CatModulo;

// Convierte entre la entidad CatModulo y sus DTOs de entrada/salida
public class CatModuloMapper {

    private CatModuloMapper() {
    }

    public static CatModulo toEntity(CatModuloRequestDTO dto) {
        return CatModulo.builder()
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
                .nombre(entidad.getNombre())
                .descripcion(entidad.getDescripcion())
                .icono(entidad.getIcono())
                .ruta(entidad.getRuta())
                .orden(entidad.getOrden())
                .estatus(entidad.getEstatus())
                .build();
    }
}
