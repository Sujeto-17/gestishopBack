package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.AdoptarCategoriasRequestDTO;
import mx.com.gestishop.application.dto.request.CategoriaRequestDTO;
import mx.com.gestishop.application.dto.response.CategoriaResponseDTO;
import mx.com.gestishop.application.dto.response.CategoriaSugeridaResponseDTO;

import java.util.List;
import java.util.UUID;

// Gestión de las categorías de productos de un negocio (familia y subfamilia)
public interface CategoriaService {

    // Todas las categorías del negocio; con soloActivas=true se omiten las inactivas (para selectores)
    List<CategoriaResponseDTO> listar(Long idNegocio, boolean soloActivas);

    CategoriaResponseDTO obtener(UUID uuidCategoria, Long idNegocio);

    CategoriaResponseDTO crear(CategoriaRequestDTO dto, Long idNegocio);

    CategoriaResponseDTO actualizar(UUID uuidCategoria, CategoriaRequestDTO dto, Long idNegocio);

    // Activa o desactiva. Desactivar una familia desactiva sus subfamilias; reactivar una subfamilia exige su familia activa
    CategoriaResponseDTO cambiarEstatus(UUID uuidCategoria, Long idNegocio);

    // Solo si no tiene subfamilias ni productos; de lo contrario se debe desactivar
    void eliminar(UUID uuidCategoria, Long idNegocio);

    // Plantillas del giro del negocio, indicando cuáles ya tiene
    List<CategoriaSugeridaResponseDTO> listarSugeridas(Long idNegocio);

    // Copia las plantillas elegidas (y las familias que necesiten) a las categorías del negocio; devuelve las creadas
    List<CategoriaResponseDTO> adoptarSugeridas(AdoptarCategoriasRequestDTO dto, Long idNegocio);
}
