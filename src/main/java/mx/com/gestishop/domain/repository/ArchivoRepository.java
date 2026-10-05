package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.Archivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Acceso a datos de la tabla archivos (genérica, polimórfica por tipo+id de entidad)
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {

    // Lista todos los archivos activos de una entidad y categoría específica, ordenados
    @Query("""
        SELECT a FROM Archivo a
        WHERE a.tipoEntidad.clave = :claveTipo
          AND a.idEntidad = :idEntidad
          AND a.categoria = :categoria
          AND a.deletedAt IS NULL
        ORDER BY a.orden ASC
        """)
    List<Archivo> listarPorEntidadYCategoria(@Param("claveTipo") String claveTipo,
                                             @Param("idEntidad") Long idEntidad,
                                             @Param("categoria") String categoria);

    // Obtiene el archivo marcado como principal de una entidad+categoría (ej. el logo vigente)
    @Query("""
        SELECT a FROM Archivo a
        WHERE a.tipoEntidad.clave = :claveTipo
          AND a.idEntidad = :idEntidad
          AND a.categoria = :categoria
          AND a.esPrincipal = true
          AND a.deletedAt IS NULL
        """)
    Optional<Archivo> buscarPrincipal(@Param("claveTipo") String claveTipo,
                                      @Param("idEntidad") Long idEntidad,
                                      @Param("categoria") String categoria);
}
