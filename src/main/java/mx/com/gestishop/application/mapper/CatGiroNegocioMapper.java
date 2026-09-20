package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.CatGiroNegocioRequestDTO;
import mx.com.gestishop.application.dto.response.CatGiroNegocioResponseDTO;
import mx.com.gestishop.domain.model.CatGiroNegocio;

// Convierte entre la entidad CatGiroNegocio y sus DTOs de entrada/salida
public class CatGiroNegocioMapper {

    private CatGiroNegocioMapper() {
    }

    // Construye una entidad nueva a partir del DTO de entrada. El estatus siempre nace 'activo'
    public static CatGiroNegocio toEntity(CatGiroNegocioRequestDTO dto) {
        return CatGiroNegocio.builder()
                .nombre(dto.getNombre())
                .icono(dto.getIcono())
                .orden(dto.getOrden() != null ? dto.getOrden() : 0)
                .estatus("activo")
                .build();
    }

    // Aplica los cambios de un DTO de entrada sobre una entidad existente (para actualizar)
    public static void actualizarEntidad(CatGiroNegocio entidad, CatGiroNegocioRequestDTO dto) {
        entidad.setNombre(dto.getNombre());
        entidad.setIcono(dto.getIcono());
        if (dto.getOrden() != null) entidad.setOrden(dto.getOrden());
    }

    public static CatGiroNegocioResponseDTO toResponse(CatGiroNegocio entidad) {
        return CatGiroNegocioResponseDTO.builder()
                .idGiro(entidad.getIdGiro())
                .nombre(entidad.getNombre())
                .icono(entidad.getIcono())
                .orden(entidad.getOrden())
                .estatus(entidad.getEstatus())
                .build();
    }
}
