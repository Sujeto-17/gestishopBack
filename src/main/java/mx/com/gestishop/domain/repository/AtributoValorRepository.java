package mx.com.gestishop.domain.repository;

import mx.com.gestishop.domain.model.AtributoValor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Acceso a datos de los valores de atributos (del sistema y propios de cada negocio)
public interface AtributoValorRepository extends JpaRepository<AtributoValor, Long> {

    // Valores del sistema (todos, activos o no) para el superadmin
    @Query("""
            SELECT v FROM AtributoValor v JOIN FETCH v.atributo
            WHERE v.negocio IS NULL
            ORDER BY v.orden ASC, v.valor ASC
            """)
    List<AtributoValor> listarDelSistema();

    // Valores que ve un negocio: los del sistema y los propios; solo activos si se indica
    @Query("""
            SELECT v FROM AtributoValor v JOIN FETCH v.atributo
            WHERE (v.negocio IS NULL OR v.negocio.idNegocio = :idNegocio)
              AND (:soloActivos = false OR v.estatus = 'activo')
            ORDER BY v.orden ASC, v.valor ASC
            """)
    List<AtributoValor> listarVisiblesParaNegocio(@Param("idNegocio") Long idNegocio,
                                                  @Param("soloActivos") boolean soloActivos);

    // Valor del sistema por su identificador público
    @Query("""
            SELECT v FROM AtributoValor v JOIN FETCH v.atributo
            WHERE v.uuidValor = :uuid AND v.negocio IS NULL
            """)
    Optional<AtributoValor> buscarDelSistema(@Param("uuid") UUID uuid);

    // Valor propio de un negocio por su identificador público (nunca devuelve el de otro negocio)
    @Query("""
            SELECT v FROM AtributoValor v JOIN FETCH v.atributo
            WHERE v.uuidValor = :uuid AND v.negocio.idNegocio = :idNegocio
            """)
    Optional<AtributoValor> buscarPropio(@Param("uuid") UUID uuid, @Param("idNegocio") Long idNegocio);

    // ¿Ya existe ese valor en el catálogo del sistema para el atributo?
    @Query("""
            SELECT COUNT(v) > 0 FROM AtributoValor v
            WHERE v.atributo.idAtributo = :idAtributo AND v.negocio IS NULL
              AND lower(v.valor) = lower(:valor) AND v.idValor <> :excluirId
            """)
    boolean existeEnSistema(@Param("idAtributo") Long idAtributo, @Param("valor") String valor,
                            @Param("excluirId") Long excluirId);

    // ¿Ya existe ese valor propio en el negocio para el atributo?
    @Query("""
            SELECT COUNT(v) > 0 FROM AtributoValor v
            WHERE v.atributo.idAtributo = :idAtributo AND v.negocio.idNegocio = :idNegocio
              AND lower(v.valor) = lower(:valor) AND v.idValor <> :excluirId
            """)
    boolean existePropio(@Param("idAtributo") Long idAtributo, @Param("idNegocio") Long idNegocio,
                         @Param("valor") String valor, @Param("excluirId") Long excluirId);

    // ¿Alguna variante usa el valor?
    @Query(value = "SELECT COUNT(*) > 0 FROM variante_valores WHERE id_valor = :idValor", nativeQuery = true)
    boolean estaEnUso(@Param("idValor") Long idValor);
}
