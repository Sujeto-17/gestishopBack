package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.AtributoRequestDTO;
import mx.com.gestishop.application.dto.request.AtributoValorRequestDTO;
import mx.com.gestishop.application.dto.response.AtributoResponseDTO;
import mx.com.gestishop.application.dto.response.AtributoValorResponseDTO;

import java.util.List;
import java.util.UUID;

// Atributos de variante: catálogo global (superadmin) y valores propios de cada negocio (admin)
public interface AtributoService {

    // --- NEGOCIO ---

    // Atributos activos con sus valores activos (del sistema + propios del negocio)
    List<AtributoResponseDTO> listarParaNegocio(Long idNegocio);

    // Agrega un valor propio a un atributo existente
    AtributoValorResponseDTO crearValorPropio(UUID uuidAtributo, AtributoValorRequestDTO dto, Long idNegocio);

    // Renombra un valor propio
    AtributoValorResponseDTO actualizarValorPropio(UUID uuidValor, AtributoValorRequestDTO dto, Long idNegocio);

    // Elimina un valor propio; si alguna variante lo usa, se desactiva
    void eliminarValorPropio(UUID uuidValor, Long idNegocio);

    // --- SUPERADMIN ---

    // Todos los atributos con sus valores del sistema (activos e inactivos)
    List<AtributoResponseDTO> listarGlobales();

    AtributoResponseDTO crearAtributo(AtributoRequestDTO dto);

    AtributoResponseDTO actualizarAtributo(UUID uuidAtributo, AtributoRequestDTO dto);

    AtributoResponseDTO cambiarEstatusAtributo(UUID uuidAtributo);

    // Elimina el atributo con sus valores; si algún producto lo usa, se desactiva
    void eliminarAtributo(UUID uuidAtributo);

    AtributoValorResponseDTO crearValorGlobal(UUID uuidAtributo, AtributoValorRequestDTO dto);

    AtributoValorResponseDTO actualizarValorGlobal(UUID uuidValor, AtributoValorRequestDTO dto);

    AtributoValorResponseDTO cambiarEstatusValorGlobal(UUID uuidValor);

    // Elimina el valor del sistema; si alguna variante lo usa, se desactiva
    void eliminarValorGlobal(UUID uuidValor);
}

