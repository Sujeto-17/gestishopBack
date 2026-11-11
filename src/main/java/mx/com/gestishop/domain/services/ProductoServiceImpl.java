package mx.com.gestishop.domain.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.ProductoDatosRequestDTO;
import mx.com.gestishop.application.dto.request.ProductoRequestDTO;
import mx.com.gestishop.application.dto.request.VarianteDatosRequestDTO;
import mx.com.gestishop.application.dto.request.VarianteRequestDTO;
import mx.com.gestishop.application.dto.response.*;
import mx.com.gestishop.application.mapper.ProductoMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.ProductoFotoService;
import mx.com.gestishop.domain.interfaces.ProductoService;
import mx.com.gestishop.domain.model.*;
import mx.com.gestishop.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reglas del módulo Productos:
 *  - Solo productos de venta; todo producto tiene al menos una variante (el simple, una sola sin atributos).
 *  - Las variantes se generan combinando un valor por cada atributo del producto; la combinación no se repite.
 *  - El precio de mayoreo (precio + cantidad) solo existe en negocios tipo "tienda" y nunca supera al menudeo.
 *  - La existencia inicial se guarda como movimiento 'inventario_inicial'; después el stock solo cambia con movimientos.
 *  - SKU y código de barras no se repiten dentro del negocio; nombre + marca + modelo tampoco.
 *  - Un producto con movimientos o solicitudes de compra no se elimina: se desactiva.
 * La BD aplica las mismas reglas (unicidad, mayoreo, aislamiento entre negocios) como última defensa.
 */
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private static final Long SIN_EXCLUIR = -1L;   // al crear no hay registro que excluir en las comparaciones
    private static final Set<Short> TASAS_IVA = Set.of((short) 0, (short) 8, (short) 16);

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CatUnidadMedidaRepository unidadRepository;
    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoFotoService fotoService;
    private final VarianteProveedorRepository costoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Variante de la petición ya validada: su firma canónica, los valores elegidos (en el orden de los atributos)
    // y el proveedor del costo inicial (null si no se capturó costo)
    private record VarianteResuelta(VarianteRequestDTO dto, String firma, List<AtributoValor> valores, Proveedor proveedor) {
    }

    // --- CONSULTAS ---

    @Override
    @Transactional(readOnly = true)
    public ProductoOpcionesResponseDTO opciones(Long idNegocio) {
        Negocio negocio = buscarNegocio(idNegocio);
        return ProductoOpcionesResponseDTO.builder()
                .permiteMayoreo(esTienda(negocio))
                .emiteFactura(Boolean.TRUE.equals(negocio.getEmiteFactura()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoListadoResponseDTO> listar(Long idNegocio, boolean soloActivos) {
        List<Producto> productos = RepositoryExecutor.execute(
                () -> productoRepository.listarPorNegocio(idNegocio, soloActivos), "Producto", "consultar");
        Map<Long, Object[]> resumen = RepositoryExecutor.execute(
                        () -> productoRepository.resumenVariantes(idNegocio), "Variante", "resumir").stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> r));
        Map<Long, String> fotos = fotoService.miniaturasPrincipales(idNegocio);
        return productos.stream()
                .map(p -> ProductoMapper.toListado(p, resumen.get(p.getIdProducto()), fotos.get(p.getIdProducto())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtener(UUID uuidProducto, Long idNegocio) {
        return aResponse(buscar(uuidProducto, idNegocio));
    }

    // --- ESCRITURA: PRODUCTO ---

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductoResponseDTO crear(ProductoRequestDTO dto, Long idNegocio, UUID uuidUsuario) {
        Negocio negocio = buscarNegocio(idNegocio);
        Categoria categoria = buscarCategoria(dto.getUuidCategoria(), idNegocio, null);
        CatUnidadMedida unidad = buscarUnidad(dto.getIdUnidad());
        validarTasaIva(dto.getTasaIva());
        validarIdentidad(idNegocio, dto, SIN_EXCLUIR);

        List<Atributo> atributos = resolverAtributos(dto.getUuidsAtributos());
        List<Long> idsAtributos = atributos.stream().map(Atributo::getIdAtributo).toList();
        List<VarianteResuelta> variantes = resolverVariantes(
                dto.getVariantes(), idsAtributos, idNegocio, esTienda(negocio), unidad, Set.of());

        Producto producto = Producto.builder()
                .negocio(negocio)
                .categoria(categoria)
                .nombre(dto.getNombre().trim())
                .marca(dto.getMarca())
                .modelo(dto.getModelo())
                .descripcion(dto.getDescripcion())
                .unidad(unidad)
                .tasaIva(dto.getTasaIva())
                .build();
        Producto guardado = RepositoryExecutor.execute(
                () -> productoRepository.save(producto), "Producto", "crear");

        for (int i = 0; i < idsAtributos.size(); i++) {
            int orden = i;
            Long idAtributo = idsAtributos.get(i);
            RepositoryExecutor.executeVoid(
                    () -> productoRepository.asociarAtributo(guardado.getIdProducto(), idAtributo, idNegocio, orden),
                    "ProductoAtributo", "crear");
        }

        Usuario usuario = variantes.stream().anyMatch(v -> existencia(v.dto()).signum() > 0)
                ? buscarUsuario(uuidUsuario) : null;
        variantes.forEach(v -> crearVariante(guardado, v, usuario));

        return recargar(guardado.getUuidProducto(), idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductoResponseDTO actualizar(UUID uuidProducto, ProductoDatosRequestDTO dto, Long idNegocio) {
        Producto producto = buscar(uuidProducto, idNegocio);
        Categoria categoria = buscarCategoria(dto.getUuidCategoria(), idNegocio, producto.getCategoria());
        CatUnidadMedida unidad = buscarUnidad(dto.getIdUnidad());
        validarTasaIva(dto.getTasaIva());
        validarIdentidad(idNegocio, dto, producto.getIdProducto());

        // Cambiar la unidad con existencias ya registradas alteraría el significado de las cantidades
        boolean cambiaUnidad = !unidad.getIdUnidad().equals(producto.getUnidad().getIdUnidad());
        if (cambiaUnidad && RepositoryExecutor.execute(
                () -> productoRepository.tieneMovimientos(producto.getIdProducto()), "Producto", "validarMovimientos")) {
            throw conflicto("No se puede cambiar la unidad de medida: el producto ya tiene movimientos de inventario.");
        }

        producto.setCategoria(categoria);
        producto.setUnidad(unidad);
        producto.setNombre(dto.getNombre().trim());
        producto.setMarca(dto.getMarca());
        producto.setModelo(dto.getModelo());
        producto.setDescripcion(dto.getDescripcion());
        producto.setTasaIva(dto.getTasaIva());

        RepositoryExecutor.execute(() -> productoRepository.save(producto), "Producto", "actualizar");
        return aResponse(producto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductoResponseDTO cambiarEstatus(UUID uuidProducto, Long idNegocio) {
        Producto producto = buscar(uuidProducto, idNegocio);
        producto.setEstatus("activo".equals(producto.getEstatus()) ? "inactivo" : "activo");
        RepositoryExecutor.execute(() -> productoRepository.save(producto), "Producto", "cambiarEstatus");
        return aResponse(producto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminar(UUID uuidProducto, Long idNegocio) {
        Producto producto = buscar(uuidProducto, idNegocio);
        Long id = producto.getIdProducto();

        boolean enUso = RepositoryExecutor.execute(() -> productoRepository.tieneMovimientos(id), "Producto", "validarMovimientos")
                || RepositoryExecutor.execute(() -> productoRepository.tieneSolicitudes(id), "Producto", "validarSolicitudes");
        if (enUso) {
            producto.setEstatus("inactivo");
            RepositoryExecutor.execute(() -> productoRepository.save(producto), "Producto", "desactivar");
            return;
        }
        // Sin movimientos: se borran las fotos, los historiales de alta, las variantes y el producto
        fotoService.eliminarTodas(id);
        RepositoryExecutor.executeVoid(() -> productoRepository.borrarHistorialPrecios(id), "Producto", "eliminar");
        RepositoryExecutor.executeVoid(() -> productoRepository.borrarHistorialCostos(id), "Producto", "eliminar");
        RepositoryExecutor.executeVoid(() -> productoRepository.borrarVariantes(id), "Producto", "eliminar");
        RepositoryExecutor.executeVoid(() -> productoRepository.delete(producto), "Producto", "eliminar");
    }

    // --- ESCRITURA: VARIANTES ---

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VarianteResponseDTO agregarVariante(UUID uuidProducto, VarianteRequestDTO dto, Long idNegocio, UUID uuidUsuario) {
        Producto producto = buscar(uuidProducto, idNegocio);
        Negocio negocio = buscarNegocio(idNegocio);
        List<Long> idsAtributos = RepositoryExecutor.execute(
                () -> productoRepository.idsAtributosDeProducto(producto.getIdProducto()), "ProductoAtributo", "consultar");
        if (idsAtributos.isEmpty()) {
            throw conflicto("Este producto no tiene atributos, por eso solo maneja una variante.");
        }
        Set<String> firmasExistentes = varianteRepository.listarPorProducto(producto.getIdProducto()).stream()
                .map(Variante::getFirma).collect(Collectors.toSet());

        VarianteResuelta resuelta = resolverVariantes(
                List.of(dto), idsAtributos, idNegocio, esTienda(negocio), producto.getUnidad(), firmasExistentes).get(0);
        Usuario usuario = existencia(dto).signum() > 0 ? buscarUsuario(uuidUsuario) : null;

        Variante nueva = crearVariante(producto, resuelta, usuario);
        entityManager.flush();
        entityManager.clear();
        Variante recargada = buscarVariante(nueva.getUuidVariante(), producto.getIdProducto(), idNegocio);
        return aResponse(recargada, valoresPorVariante(producto.getIdProducto()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VarianteResponseDTO actualizarVariante(UUID uuidProducto, UUID uuidVariante, VarianteDatosRequestDTO dto, Long idNegocio) {
        Producto producto = buscar(uuidProducto, idNegocio);
        Variante variante = buscarVariante(uuidVariante, producto.getIdProducto(), idNegocio);
        validarDatosVariante(dto, esTienda(buscarNegocio(idNegocio)), producto.getUnidad());
        validarIdentificadores(dto, idNegocio, variante.getIdVariante());

        variante.setSku(dto.getSku());
        variante.setCodigoBarras(dto.getCodigoBarras());
        variante.setPrecioPublico(dto.getPrecioPublico());
        variante.setPrecioMayoreo(dto.getPrecioMayoreo());
        variante.setCantidadMayoreo(dto.getCantidadMayoreo());
        variante.setStockMinimo(dto.getStockMinimo() == null ? BigDecimal.ZERO : dto.getStockMinimo());

        RepositoryExecutor.execute(() -> varianteRepository.save(variante), "Variante", "actualizar");
        return aResponse(variante, valoresPorVariante(producto.getIdProducto()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VarianteResponseDTO cambiarEstatusVariante(UUID uuidProducto, UUID uuidVariante, Long idNegocio) {
        Producto producto = buscar(uuidProducto, idNegocio);
        Variante variante = buscarVariante(uuidVariante, producto.getIdProducto(), idNegocio);
        variante.setEstatus("activo".equals(variante.getEstatus()) ? "inactivo" : "activo");
        RepositoryExecutor.execute(() -> varianteRepository.save(variante), "Variante", "cambiarEstatus");
        return aResponse(variante, valoresPorVariante(producto.getIdProducto()));
    }

    // --- VALIDACIÓN Y RESOLUCIÓN ---

    // Atributos activos elegidos (sin repetir) en el orden recibido
    private List<Atributo> resolverAtributos(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return List.of();
        List<UUID> distintos = uuids.stream().distinct().toList();
        if (distintos.size() != uuids.size()) {
            throw conflicto("Un atributo está repetido en el producto.");
        }
        Map<UUID, Atributo> encontrados = RepositoryExecutor.execute(
                        () -> productoRepository.atributosActivos(distintos), "Atributo", "consultar").stream()
                .collect(Collectors.toMap(Atributo::getUuidAtributo, a -> a));
        List<Atributo> ordenados = new ArrayList<>();
        for (UUID uuid : distintos) {
            Atributo a = encontrados.get(uuid);
            if (a == null) throw conflicto("Uno de los atributos no existe o no está disponible.");
            ordenados.add(a);
        }
        return ordenados;
    }

    /**
     * Valida todas las variantes de la petición y calcula la firma de cada una.
     * firmasExistentes son las ya registradas en el producto (vacío al crear).
     */
    private List<VarianteResuelta> resolverVariantes(List<VarianteRequestDTO> lista, List<Long> idsAtributos,
                                                     Long idNegocio, boolean esTienda, CatUnidadMedida unidad,
                                                     Set<String> firmasExistentes) {
        if (lista == null || lista.isEmpty()) {
            throw conflicto("El producto necesita al menos una variante.");
        }
        if (idsAtributos.isEmpty() && lista.size() != 1) {
            throw conflicto("Un producto sin atributos tiene una sola variante.");
        }

        // Catálogo de valores usables por el negocio, consultado una sola vez
        Set<UUID> uuidsValores = lista.stream()
                .flatMap(v -> v.getUuidsValores() == null ? java.util.stream.Stream.<UUID>empty() : v.getUuidsValores().stream())
                .collect(Collectors.toSet());
        Map<UUID, AtributoValor> catalogo = uuidsValores.isEmpty() ? Map.of()
                : RepositoryExecutor.execute(() -> productoRepository.valoresUsables(uuidsValores, idNegocio), "AtributoValor", "consultar")
                .stream().collect(Collectors.toMap(AtributoValor::getUuidValor, v -> v));

        Map<UUID, Proveedor> proveedores = resolverProveedores(lista, idNegocio);

        List<VarianteResuelta> resueltas = new ArrayList<>();
        Set<String> firmas = new HashSet<>(firmasExistentes);
        Set<String> skus = new HashSet<>();
        Set<String> barras = new HashSet<>();

        for (VarianteRequestDTO dto : lista) {
            VarianteResuelta r = resolverUna(dto, idsAtributos, catalogo);
            r = new VarianteResuelta(r.dto(), r.firma(), r.valores(),
                    dto.getUuidProveedor() == null ? null : proveedores.get(dto.getUuidProveedor()));
            if (!firmas.add(r.firma())) {
                throw conflicto("La combinación " + etiqueta(r.valores()) + " ya existe en el producto.");
            }
            validarDatosVariante(dto, esTienda, unidad);
            validarCantidad(existencia(dto), "La existencia inicial", unidad);
            if (dto.getSku() != null && !skus.add(dto.getSku().toLowerCase())) {
                throw conflicto("El SKU \"" + dto.getSku() + "\" está repetido en las variantes.");
            }
            if (dto.getCodigoBarras() != null && !barras.add(dto.getCodigoBarras())) {
                throw conflicto("El código de barras \"" + dto.getCodigoBarras() + "\" está repetido en las variantes.");
            }
            validarIdentificadores(dto, idNegocio, SIN_EXCLUIR);
            resueltas.add(r);
        }
        return resueltas;
    }

    /**
     * Proveedores de los costos iniciales, consultados una sola vez. Cada variante debe traer proveedor y costo
     * juntos o ninguno de los dos; el proveedor debe ser del negocio y estar activo.
     */
    private Map<UUID, Proveedor> resolverProveedores(List<VarianteRequestDTO> lista, Long idNegocio) {
        Map<UUID, Proveedor> proveedores = new HashMap<>();
        for (VarianteRequestDTO dto : lista) {
            if ((dto.getUuidProveedor() == null) != (dto.getCosto() == null)) {
                throw conflicto("El costo necesita proveedor, o ninguno de los dos.");
            }
            if (dto.getUuidProveedor() == null || proveedores.containsKey(dto.getUuidProveedor())) continue;
            UUID uuid = dto.getUuidProveedor();
            Proveedor proveedor = RepositoryExecutor.execute(
                    () -> proveedorRepository.buscarPorUuid(uuid, idNegocio)
                            .orElseThrow(() -> ErrorFactory.notFound("Proveedor", uuid)),
                    "Proveedor", "consultar");
            if (!"activo".equals(proveedor.getEstatus())) {
                throw conflicto("El proveedor \"" + proveedor.getNombre() + "\" está inactivo.");
            }
            proveedores.put(uuid, proveedor);
        }
        return proveedores;
    }

    // Exige exactamente un valor por atributo del producto, usable por el negocio, sin repetir atributo
    private VarianteResuelta resolverUna(VarianteRequestDTO dto, List<Long> idsAtributos, Map<UUID, AtributoValor> catalogo) {
        List<UUID> uuids = dto.getUuidsValores() == null ? List.of() : dto.getUuidsValores();
        if (uuids.size() != idsAtributos.size()) {
            throw conflicto("Cada variante necesita un valor por cada atributo del producto (" + idsAtributos.size() + ").");
        }
        Map<Long, AtributoValor> porAtributo = new HashMap<>();
        for (UUID uuid : uuids) {
            AtributoValor valor = catalogo.get(uuid);
            if (valor == null) throw conflicto("Uno de los valores no existe o no está disponible.");
            Long idAtributo = valor.getAtributo().getIdAtributo();
            if (!idsAtributos.contains(idAtributo)) {
                throw conflicto("El valor \"" + valor.getValor() + "\" no corresponde a los atributos del producto.");
            }
            if (porAtributo.put(idAtributo, valor) != null) {
                throw conflicto("Una variante tiene dos valores de " + valor.getAtributo().getNombre() + ".");
            }
        }
        // Firma canónica: atributo:valor ordenado por id de atributo
        String firma = porAtributo.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ":" + e.getValue().getIdValor())
                .collect(Collectors.joining("|"));
        List<AtributoValor> ordenados = idsAtributos.stream().map(porAtributo::get).toList();
        return new VarianteResuelta(dto, firma, ordenados, null);
    }

    // Reglas de precios y cantidades de una variante
    private void validarDatosVariante(VarianteDatosRequestDTO dto, boolean esTienda, CatUnidadMedida unidad) {
        boolean hayPrecio = dto.getPrecioMayoreo() != null;
        boolean hayCantidad = dto.getCantidadMayoreo() != null;
        if (hayPrecio != hayCantidad) {
            throw conflicto("El mayoreo necesita precio y cantidad mínima, o ninguno de los dos.");
        }
        if (hayPrecio) {
            if (!esTienda) {
                throw conflicto("Este tipo de negocio no maneja precio de mayoreo en productos.");
            }
            if (dto.getPrecioMayoreo().compareTo(dto.getPrecioPublico()) > 0) {
                throw conflicto("El precio de mayoreo no puede ser mayor al precio al público.");
            }
            validarCantidad(dto.getCantidadMayoreo(), "La cantidad de mayoreo", unidad);
        }
        if (dto.getStockMinimo() != null) validarCantidad(dto.getStockMinimo(), "El stock mínimo", unidad);
    }

    // Si la unidad no admite decimales (piezas), la cantidad debe ser entera
    private void validarCantidad(BigDecimal cantidad, String campo, CatUnidadMedida unidad) {
        if (cantidad == null) return;
        boolean admiteDecimales = Boolean.TRUE.equals(unidad.getPermiteDecimales());
        if (!admiteDecimales && cantidad.stripTrailingZeros().scale() > 0) {
            throw conflicto(campo + " debe ser un número entero: la unidad \"" + unidad.getNombre() + "\" no admite decimales.");
        }
    }

    // SKU y código de barras no se repiten en el negocio
    private void validarIdentificadores(VarianteDatosRequestDTO dto, Long idNegocio, Long excluirId) {
        if (dto.getSku() != null && RepositoryExecutor.execute(
                () -> varianteRepository.existeSku(idNegocio, dto.getSku(), excluirId), "Variante", "validarSku")) {
            throw conflicto("El SKU \"" + dto.getSku() + "\" ya está registrado en otra variante.");
        }
        if (dto.getCodigoBarras() != null && RepositoryExecutor.execute(
                () -> varianteRepository.existeCodigoBarras(idNegocio, dto.getCodigoBarras(), excluirId), "Variante", "validarBarras")) {
            throw conflicto("El código de barras \"" + dto.getCodigoBarras() + "\" ya está registrado en otra variante.");
        }
    }

    private void validarIdentidad(Long idNegocio, ProductoDatosRequestDTO dto, Long excluirId) {
        boolean existe = RepositoryExecutor.execute(() -> productoRepository.existeIdentidad(
                idNegocio, dto.getNombre().trim(),
                dto.getMarca() == null ? "" : dto.getMarca(),
                dto.getModelo() == null ? "" : dto.getModelo(), excluirId), "Producto", "validarIdentidad");
        if (existe) {
            throw conflicto("Ya existe un producto con el mismo nombre, marca y modelo. "
                    + "Si solo cambia la presentación, agrégala como variante.");
        }
    }

    private void validarTasaIva(Short tasa) {
        if (!TASAS_IVA.contains(tasa)) {
            throw conflicto("La tasa de IVA debe ser 0, 8 o 16.");
        }
    }

    // --- PERSISTENCIA DE VARIANTES ---

    // Guarda la variante, sus valores, el costo inicial (si trae proveedor) y, si trae existencia, el movimiento de inventario inicial
    private Variante crearVariante(Producto producto, VarianteResuelta r, Usuario usuario) {
        VarianteRequestDTO dto = r.dto();
        Variante variante = Variante.builder()
                .producto(producto)
                .idNegocio(producto.getNegocio().getIdNegocio())
                .sku(dto.getSku())
                .codigoBarras(dto.getCodigoBarras())
                .firma(r.firma())
                .precioPublico(dto.getPrecioPublico())
                .precioMayoreo(dto.getPrecioMayoreo())
                .cantidadMayoreo(dto.getCantidadMayoreo())
                .stockMinimo(dto.getStockMinimo() == null ? BigDecimal.ZERO : dto.getStockMinimo())
                .build();
        Variante guardada = RepositoryExecutor.execute(() -> varianteRepository.save(variante), "Variante", "crear");

        for (AtributoValor valor : r.valores()) {
            RepositoryExecutor.executeVoid(() -> varianteRepository.asignarValor(
                    guardada.getIdVariante(), producto.getIdProducto(),
                    valor.getAtributo().getIdAtributo(), valor.getIdValor()), "VarianteValor", "crear");
        }

        // Costo inicial opcional: queda como el proveedor preferido de la variante
        if (r.proveedor() != null) {
            VarianteProveedor costo = VarianteProveedor.builder()
                    .variante(guardada)
                    .proveedor(r.proveedor())
                    .idNegocio(guardada.getIdNegocio())
                    .costo(dto.getCosto())
                    .esPreferido(true)
                    .build();
            RepositoryExecutor.execute(() -> costoRepository.save(costo), "VarianteProveedor", "crear");
        }

        BigDecimal existencia = existencia(dto);
        if (existencia.signum() > 0) {
            MovimientoInventario movimiento = MovimientoInventario.builder()
                    .idNegocio(guardada.getIdNegocio())
                    .variante(guardada)
                    .tipo("entrada")
                    .motivo("inventario_inicial")
                    .cantidad(existencia)
                    .usuario(usuario)
                    .build();
            RepositoryExecutor.execute(() -> movimientoRepository.save(movimiento), "MovimientoInventario", "crear");
        }
        return guardada;
    }

    private BigDecimal existencia(VarianteRequestDTO dto) {
        return dto.getExistenciaInicial() == null ? BigDecimal.ZERO : dto.getExistenciaInicial();
    }

    // El trigger de movimientos actualiza el stock en la BD: se vacía la caché para leer el valor real
    private ProductoResponseDTO recargar(UUID uuidProducto, Long idNegocio) {
        entityManager.flush();
        entityManager.clear();
        return aResponse(buscar(uuidProducto, idNegocio));
    }

    // --- BÚSQUEDAS ---

    private Producto buscar(UUID uuid, Long idNegocio) {
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

    private Negocio buscarNegocio(Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");
    }

    private Usuario buscarUsuario(UUID uuidUsuario) {
        return RepositoryExecutor.execute(
                () -> usuarioRepository.buscarPorUuid(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Usuario", uuidUsuario)),
                "Usuario", "consultar");
    }

    // La categoría debe ser del negocio y estar activa (salvo que sea la que el producto ya tenía)
    private Categoria buscarCategoria(UUID uuid, Long idNegocio, Categoria actual) {
        Categoria categoria = RepositoryExecutor.execute(
                () -> categoriaRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Categoria", uuid)),
                "Categoria", "consultar");
        boolean esLaActual = actual != null && actual.getIdCategoria().equals(categoria.getIdCategoria());
        if (!esLaActual && !"activo".equals(categoria.getEstatus())) {
            throw conflicto("La categoría \"" + categoria.getNombre() + "\" está inactiva.");
        }
        return categoria;
    }

    private CatUnidadMedida buscarUnidad(Long idUnidad) {
        CatUnidadMedida unidad = RepositoryExecutor.execute(
                () -> unidadRepository.findById(idUnidad)
                        .orElseThrow(() -> ErrorFactory.notFound("Unidad de medida", idUnidad)),
                "CatUnidadMedida", "consultar");
        if (!"activo".equals(unidad.getEstatus())) {
            throw conflicto("La unidad de medida \"" + unidad.getNombre() + "\" no está disponible.");
        }
        return unidad;
    }

    // Los negocios tipo "tienda" son los únicos con precio de mayoreo
    private boolean esTienda(Negocio negocio) {
        return "tienda".equals(negocio.getSistemaType());
    }

    // --- RESPUESTAS ---

    private ProductoResponseDTO aResponse(Producto producto) {
        List<ProductoAtributoResponseDTO> atributos = RepositoryExecutor.execute(
                        () -> productoRepository.atributosDeProducto(producto.getIdProducto()), "ProductoAtributo", "consultar")
                .stream()
                .map(r -> ProductoAtributoResponseDTO.builder()
                        .uuidAtributo(UUID.fromString(String.valueOf(r[1])))
                        .nombre((String) r[2])
                        .build())
                .toList();

        Map<Long, List<VarianteValorResponseDTO>> valores = valoresPorVariante(producto.getIdProducto());
        List<VarianteResponseDTO> variantes = RepositoryExecutor.execute(
                        () -> varianteRepository.listarPorProducto(producto.getIdProducto()), "Variante", "consultar")
                .stream()
                .map(v -> aResponse(v, valores))
                .toList();
        return ProductoMapper.toResponse(producto, atributos, variantes);
    }

    private VarianteResponseDTO aResponse(Variante variante, Map<Long, List<VarianteValorResponseDTO>> valores) {
        return ProductoMapper.toResponse(variante, valores.getOrDefault(variante.getIdVariante(), List.of()));
    }

    // Valores de todas las variantes del producto agrupados por variante, en el orden de los atributos
    private Map<Long, List<VarianteValorResponseDTO>> valoresPorVariante(Long idProducto) {
        return RepositoryExecutor.execute(
                        () -> varianteRepository.valoresDeProducto(idProducto), "VarianteValor", "consultar").stream()
                .collect(Collectors.groupingBy(r -> ((Number) r[0]).longValue(),
                        LinkedHashMap::new,
                        Collectors.mapping(r -> VarianteValorResponseDTO.builder()
                                .uuidAtributo(UUID.fromString(String.valueOf(r[1])))
                                .atributo((String) r[2])
                                .uuidValor(UUID.fromString(String.valueOf(r[3])))
                                .valor((String) r[4])
                                .build(), Collectors.toList())));
    }

    private String etiqueta(List<AtributoValor> valores) {
        return valores.stream().map(AtributoValor::getValor).collect(Collectors.joining(" / "));
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
