package mx.com.gestishop.domain.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.VarianteProveedorRequestDTO;
import mx.com.gestishop.application.dto.response.HistorialCostoResponseDTO;
import mx.com.gestishop.application.dto.response.ProveedorCostoResponseDTO;
import mx.com.gestishop.application.dto.response.VarianteCostosResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.core.utils.DateUtils;
import mx.com.gestishop.domain.interfaces.VarianteCostoService;
import mx.com.gestishop.domain.model.Producto;
import mx.com.gestishop.domain.model.Proveedor;
import mx.com.gestishop.domain.model.Variante;
import mx.com.gestishop.domain.model.VarianteProveedor;
import mx.com.gestishop.domain.repository.ProductoRepository;
import mx.com.gestishop.domain.repository.ProveedorRepository;
import mx.com.gestishop.domain.repository.VarianteProveedorRepository;
import mx.com.gestishop.domain.repository.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reglas de costos por variante y proveedor:
 *  - Cada variante puede tener varios proveedores, cada uno con su costo.
 *  - Solo uno es el preferido; el primero que se registra lo es automáticamente.
 *  - La ganancia se calcula con el costo del proveedor preferido.
 *  - Solo se pueden agregar proveedores activos del mismo negocio.
 *  - Los cambios de costo quedan en variante_costos_historial (lo registra un trigger de la BD).
 */
@Service
@RequiredArgsConstructor
public class VarianteCostoServiceImpl implements VarianteCostoService {

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final VarianteProveedorRepository costoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // --- CONSULTAS ---

    // Variantes del producto con su precio, sus proveedores y la ganancia
    @Override
    @Transactional(readOnly = true)
    public List<VarianteCostosResponseDTO> listar(UUID uuidProducto, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        return armar(producto.getIdProducto(), idNegocio);
    }

    // Últimos cambios de costo de la variante
    @Override
    @Transactional(readOnly = true)
    public List<HistorialCostoResponseDTO> historial(UUID uuidProducto, UUID uuidVariante, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Variante variante = buscarVariante(uuidVariante, producto.getIdProducto(), idNegocio);
        return RepositoryExecutor.execute(
                        () -> costoRepository.historial(variante.getIdVariante(), idNegocio), "VarianteProveedor", "historial")
                .stream()
                .map(r -> HistorialCostoResponseDTO.builder()
                        .fecha(DateUtils.aOffsetDateTime(r[0]))
                        .uuidProveedor(UUID.fromString(String.valueOf(r[1])))
                        .proveedor((String) r[2])
                        .costoAnterior((BigDecimal) r[3])
                        .costoNuevo((BigDecimal) r[4])
                        .build())
                .toList();
    }

    // --- ESCRITURA ---

    // Registra o actualiza el costo de una variante con un proveedor
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VarianteCostosResponseDTO guardar(UUID uuidProducto, UUID uuidVariante, VarianteProveedorRequestDTO dto, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Variante variante = buscarVariante(uuidVariante, idProducto, idNegocio);
        Long idVariante = variante.getIdVariante();
        Proveedor proveedor = buscarProveedor(dto.getUuidProveedor(), idNegocio);

        aplicar(idVariante, idNegocio, proveedor, dto);
        return unaVariante(idProducto, idNegocio, uuidVariante);
    }

    // Aplica el mismo proveedor y costo a todas las variantes del producto
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<VarianteCostosResponseDTO> guardarEnTodas(UUID uuidProducto, VarianteProveedorRequestDTO dto, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Proveedor proveedor = buscarProveedor(dto.getUuidProveedor(), idNegocio);
        List<Long> idsVariantes = RepositoryExecutor.execute(
                        () -> varianteRepository.listarPorProducto(idProducto), "Variante", "consultar")
                .stream().map(Variante::getIdVariante).toList();

        for (Long idVariante : idsVariantes) {
            aplicar(idVariante, idNegocio, proveedor, dto);
        }
        return armar(idProducto, idNegocio);
    }

    // Marca al proveedor como el preferido de la variante
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VarianteCostosResponseDTO marcarPreferido(UUID uuidProducto, UUID uuidVariante, UUID uuidProveedor, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Variante variante = buscarVariante(uuidVariante, idProducto, idNegocio);
        Long idVariante = variante.getIdVariante();
        Proveedor proveedor = buscarProveedor(uuidProveedor, idNegocio);
        Long idProveedor = proveedor.getIdProveedor();

        VarianteProveedor actual = buscarCosto(idVariante, idProveedor, uuidProveedor);
        if (!Boolean.TRUE.equals(actual.getEsPreferido())) {
            hacerPreferido(idVariante, idProveedor);
        }
        return unaVariante(idProducto, idNegocio, uuidVariante);
    }

    // Quita al proveedor de la variante; si era el preferido, pasa a serlo el de menor costo
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VarianteCostosResponseDTO quitar(UUID uuidProducto, UUID uuidVariante, UUID uuidProveedor, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Variante variante = buscarVariante(uuidVariante, idProducto, idNegocio);
        Long idVariante = variante.getIdVariante();
        Proveedor proveedor = buscarProveedor(uuidProveedor, idNegocio);

        VarianteProveedor actual = buscarCosto(idVariante, proveedor.getIdProveedor(), uuidProveedor);
        boolean eraPreferido = Boolean.TRUE.equals(actual.getEsPreferido());
        RepositoryExecutor.executeVoid(() -> costoRepository.delete(actual), "VarianteProveedor", "eliminar");
        entityManager.flush();

        if (eraPreferido) {
            // La lista llega ordenada por costo ascendente: el primero es el más barato
            List<VarianteProveedor> restantes = costoRepository.listarPorVariante(idVariante);
            if (!restantes.isEmpty()) {
                restantes.get(0).setEsPreferido(true);
                entityManager.flush();
            }
        }
        entityManager.clear();
        return unaVariante(idProducto, idNegocio, uuidVariante);
    }

    // Registra el costo de una compra: crea el proveedor de la variante si no estaba o actualiza su costo
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registrarCostoDeCompra(Long idVariante, UUID uuidProveedor, BigDecimal costo, Long idNegocio) {
        Proveedor proveedor = buscarProveedor(uuidProveedor, idNegocio);
        Optional<VarianteProveedor> existente = RepositoryExecutor.execute(
                () -> costoRepository.buscar(idVariante, proveedor.getIdProveedor()), "VarianteProveedor", "consultar");
        // Un proveedor inactivo no se agrega como nuevo; el costo de la compra queda solo en el movimiento
        if (existente.isEmpty() && !"activo".equals(proveedor.getEstatus())) return;

        // Se conservan el código, las notas y la última compra que ya tuviera el proveedor
        VarianteProveedorRequestDTO dto = new VarianteProveedorRequestDTO();
        dto.setCosto(costo);
        existente.ifPresent(e -> {
            dto.setCodigoProveedor(e.getCodigoProveedor());
            dto.setNotas(e.getNotas());
            dto.setUltimaCompra(e.getUltimaCompra());
        });
        aplicar(idVariante, idNegocio, proveedor, dto);
    }

    // --- LÓGICA INTERNA ---

    /**
     * Crea o actualiza el costo de la variante con el proveedor.
     * El primer proveedor de una variante es siempre el preferido; con esPreferido=true también pasa a serlo.
     * Quitar la marca de preferido a uno se hace marcando otro, nunca directamente.
     */
    private void aplicar(Long idVariante, Long idNegocio, Proveedor proveedor, VarianteProveedorRequestDTO dto) {
        Long idProveedor = proveedor.getIdProveedor();
        Optional<VarianteProveedor> existente = RepositoryExecutor.execute(
                () -> costoRepository.buscar(idVariante, idProveedor), "VarianteProveedor", "consultar");
        if (existente.isEmpty() && !"activo".equals(proveedor.getEstatus())) {
            throw conflicto("El proveedor \"" + proveedor.getNombre() + "\" está inactivo.");
        }
        long total = RepositoryExecutor.execute(
                () -> costoRepository.contarPorVariante(idVariante), "VarianteProveedor", "contar");
        boolean yaEsPreferido = existente.map(e -> Boolean.TRUE.equals(e.getEsPreferido())).orElse(false);
        boolean serPreferido = yaEsPreferido || Boolean.TRUE.equals(dto.getEsPreferido()) || total == 0;

        // Con otro proveedor marcado, primero se le quita la marca (el índice permite solo uno por variante)
        if (serPreferido && !yaEsPreferido && total > 0) {
            costoRepository.quitarPreferido(idVariante);
        }

        VarianteProveedor costo = costoRepository.buscar(idVariante, idProveedor).orElseGet(() ->
                VarianteProveedor.builder()
                        .variante(entityManager.getReference(Variante.class, idVariante))
                        .proveedor(entityManager.getReference(Proveedor.class, idProveedor))
                        .idNegocio(idNegocio)
                        .build());
        costo.setCosto(dto.getCosto());
        costo.setCodigoProveedor(dto.getCodigoProveedor());
        costo.setNotas(dto.getNotas());
        costo.setUltimaCompra(dto.getUltimaCompra());
        costo.setEsPreferido(serPreferido);
        RepositoryExecutor.execute(() -> costoRepository.save(costo), "VarianteProveedor", "guardar");
        entityManager.flush();
    }

    // Pasa el proveedor a preferido dejando a los demás sin la marca
    private void hacerPreferido(Long idVariante, Long idProveedor) {
        costoRepository.quitarPreferido(idVariante);
        VarianteProveedor costo = costoRepository.buscar(idVariante, idProveedor).orElseThrow();
        costo.setEsPreferido(true);
        RepositoryExecutor.execute(() -> costoRepository.save(costo), "VarianteProveedor", "marcarPreferido");
        entityManager.flush();
    }

    // Lee de nuevo (sin caché) una variante del producto con sus costos
    private VarianteCostosResponseDTO unaVariante(Long idProducto, Long idNegocio, UUID uuidVariante) {
        entityManager.flush();
        entityManager.clear();
        return armar(idProducto, idNegocio).stream()
                .filter(v -> v.getUuidVariante().equals(uuidVariante))
                .findFirst()
                .orElseThrow(() -> ErrorFactory.notFound("Variante", uuidVariante));
    }

    // Arma la respuesta de todas las variantes del producto: precio, proveedores, costo preferido y ganancia
    private List<VarianteCostosResponseDTO> armar(Long idProducto, Long idNegocio) {
        List<Variante> variantes = RepositoryExecutor.execute(
                () -> varianteRepository.listarPorProducto(idProducto), "Variante", "consultar");
        Map<Long, List<VarianteProveedor>> costos = RepositoryExecutor.execute(
                        () -> costoRepository.listarPorProducto(idProducto, idNegocio), "VarianteProveedor", "consultar")
                .stream().collect(Collectors.groupingBy(c -> c.getVariante().getIdVariante()));
        Map<Long, String> etiquetas = etiquetasPorVariante(idProducto);

        return variantes.stream().map(v -> {
            List<VarianteProveedor> lista = costos.getOrDefault(v.getIdVariante(), List.of());
            VarianteProveedor preferido = lista.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getEsPreferido())).findFirst().orElse(null);
            BigDecimal costoPreferido = preferido == null ? null : preferido.getCosto();
            BigDecimal ganancia = costoPreferido == null ? null : v.getPrecioPublico().subtract(costoPreferido);
            BigDecimal margen = ganancia == null || v.getPrecioPublico().signum() == 0 ? null
                    : ganancia.multiply(BigDecimal.valueOf(100)).divide(v.getPrecioPublico(), 1, RoundingMode.HALF_UP);
            return VarianteCostosResponseDTO.builder()
                    .uuidVariante(v.getUuidVariante())
                    .etiqueta(etiquetas.getOrDefault(v.getIdVariante(), ""))
                    .estatus(v.getEstatus())
                    .precioPublico(v.getPrecioPublico())
                    .precioMayoreo(v.getPrecioMayoreo())
                    .costoPreferido(costoPreferido)
                    .gananciaMonto(ganancia)
                    .margenPorcentaje(margen)
                    .proveedores(lista.stream().map(this::aProveedorCosto).toList())
                    .build();
        }).toList();
    }

    // Texto de cada variante ("Rojo / M") unido desde sus valores, en el orden de los atributos
    private Map<Long, String> etiquetasPorVariante(Long idProducto) {
        return RepositoryExecutor.execute(
                        () -> varianteRepository.valoresDeProducto(idProducto), "VarianteValor", "consultar").stream()
                .collect(Collectors.groupingBy(r -> ((Number) r[0]).longValue(),
                        LinkedHashMap::new,
                        Collectors.mapping(r -> (String) r[4], Collectors.joining(" / "))));
    }

    // Convierte un costo registrado a su respuesta
    private ProveedorCostoResponseDTO aProveedorCosto(VarianteProveedor c) {
        return ProveedorCostoResponseDTO.builder()
                .uuidProveedor(c.getProveedor().getUuidProveedor())
                .proveedor(c.getProveedor().getNombre())
                .costo(c.getCosto())
                .preciosIncluyenIva(c.getProveedor().getPreciosIncluyenIva())
                .codigoProveedor(c.getCodigoProveedor())
                .esPreferido(c.getEsPreferido())
                .notas(c.getNotas())
                .ultimaCompra(c.getUltimaCompra())
                .build();
    }

    // --- BÚSQUEDAS ---

    private Producto buscarProducto(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> productoRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Producto", uuid)),
                "Producto", "consultar");
    }

    private Variante buscarVariante(UUID uuid, Long idProducto, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> varianteRepository.buscarPorUuid(uuid, idProducto, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Variante", uuid)),
                "Variante", "consultar");
    }

    private Proveedor buscarProveedor(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> proveedorRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Proveedor", uuid)),
                "Proveedor", "consultar");
    }

    // El costo de la variante con ese proveedor; si no existe, se informa que no lo surte
    private VarianteProveedor buscarCosto(Long idVariante, Long idProveedor, UUID uuidProveedor) {
        return RepositoryExecutor.execute(
                () -> costoRepository.buscar(idVariante, idProveedor)
                        .orElseThrow(() -> ErrorFactory.notFound("Proveedor de la variante", uuidProveedor)),
                "VarianteProveedor", "consultar");
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
