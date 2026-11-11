package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.response.ArchivoResponseDTO;
import mx.com.gestishop.application.dto.response.FotoProductoResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.core.service.AlmacenImagenes;
import mx.com.gestishop.domain.interfaces.ArchivoService;
import mx.com.gestishop.domain.interfaces.ProductoFotoService;
import mx.com.gestishop.domain.model.Archivo;
import mx.com.gestishop.domain.model.Producto;
import mx.com.gestishop.domain.repository.FotoProductoRepository;
import mx.com.gestishop.domain.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reglas de las fotos de producto:
 *  - Máximo 5 por producto (1 principal + 4 más); el límite se valida en el servidor con la fila del producto bloqueada.
 *  - La primera foto que se sube queda como principal; solo puede haber una principal.
 *  - El archivo se valida, reduce y guarda con ArchivoService/AlmacenImagenes (JPG/PNG, 5 MB, 1200 px + miniatura de 300 px)
 *    en negocios/{idNegocio}/productos/{idProducto}/.
 *  - Al borrar, la fila se marca como borrada y los archivos se eliminan del disco hasta que la transacción se confirma.
 *  - El producto debe pertenecer al negocio de la sesión.
 */
@Service
@RequiredArgsConstructor
public class ProductoFotoServiceImpl implements ProductoFotoService {

    // Claves de la tabla archivos para las fotos de producto
    private static final String TIPO_ENTIDAD = "producto";
    private static final String CATEGORIA = "foto_producto";
    // Prefijo del endpoint público que sirve las imágenes (igual que el logo del negocio)
    private static final String PREFIJO_VER = "/api/v1/archivos/ver/";

    private final ProductoRepository productoRepository;
    private final FotoProductoRepository fotoRepository;
    private final ArchivoService archivoService;
    private final AlmacenImagenes almacenImagenes;

    // --- CONSULTAS ---

    // Fotos del producto: primero la principal y luego las demás por su orden
    @Override
    @Transactional(readOnly = true)
    public List<FotoProductoResponseDTO> listar(UUID uuidProducto, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        return fotosDe(producto.getIdProducto());
    }

    // Miniatura de la foto principal de cada producto del negocio (idProducto → ruta lista para /archivos/ver)
    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> miniaturasPrincipales(Long idNegocio) {
        return RepositoryExecutor.execute(
                        () -> fotoRepository.principalesDelNegocio(idNegocio), "Archivo", "listar").stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> PREFIJO_VER + miniaturaDe(r[1] + "/" + r[2]),
                        (a, b) -> a));
    }

    // --- ESCRITURA ---

    // Sube una foto; la primera que se sube queda como principal
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FotoProductoResponseDTO> subir(UUID uuidProducto, MultipartFile archivo, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();

        // Con la fila bloqueada, dos subidas a la vez no pueden pasar del límite
        fotoRepository.bloquearProducto(idProducto);
        List<Archivo> actuales = fotoRepository.listar(idProducto);
        if (actuales.size() >= MAX_FOTOS) {
            throw conflicto("Un producto admite máximo " + MAX_FOTOS + " fotos (1 principal y " + (MAX_FOTOS - 1)
                    + " más). Borra una para subir otra.");
        }

        boolean primera = actuales.isEmpty();
        ArchivoResponseDTO guardada = archivoService.subirDeNegocio(
                TIPO_ENTIDAD, idNegocio, idProducto, CATEGORIA, archivo, primera);

        // Va al final de las demás
        short siguienteOrden = (short) (actuales.stream().mapToInt(a -> a.getOrden()).max().orElse(-1) + 1);
        Archivo nueva = fotoRepository.buscarPorUuid(idProducto, guardada.getUuidArchivo())
                .orElseThrow(() -> ErrorFactory.notFound("Foto", guardada.getUuidArchivo()));
        nueva.setOrden(siguienteOrden);
        return fotosDe(idProducto);
    }

    // Pasa la foto a ser la principal; la principal anterior queda entre las demás
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FotoProductoResponseDTO> marcarPrincipal(UUID uuidProducto, UUID uuidFoto, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Archivo elegida = buscarFoto(idProducto, uuidFoto);

        for (Archivo foto : fotoRepository.listar(idProducto)) {
            foto.setEsPrincipal(foto.getIdArchivo().equals(elegida.getIdArchivo()));
        }
        return fotosDe(idProducto);
    }

    // Define el orden de las fotos; la lista debe traer exactamente las fotos del producto
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FotoProductoResponseDTO> reordenar(UUID uuidProducto, List<UUID> uuidsFotos, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Map<UUID, Archivo> porUuid = fotoRepository.listar(idProducto).stream()
                .collect(Collectors.toMap(Archivo::getUuidArchivo, a -> a));

        if (uuidsFotos.size() != porUuid.size() || !porUuid.keySet().equals(new HashSet<>(uuidsFotos))) {
            throw conflicto("La lista debe incluir todas las fotos del producto, sin repetir.");
        }
        for (int i = 0; i < uuidsFotos.size(); i++) {
            porUuid.get(uuidsFotos.get(i)).setOrden((short) i);
        }
        return fotosDe(idProducto);
    }

    // Borra una foto; si era la principal, pasa a serlo la siguiente
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FotoProductoResponseDTO> eliminar(UUID uuidProducto, UUID uuidFoto, Long idNegocio) {
        Producto producto = buscarProducto(uuidProducto, idNegocio);
        Long idProducto = producto.getIdProducto();
        Archivo foto = buscarFoto(idProducto, uuidFoto);
        boolean eraPrincipal = Boolean.TRUE.equals(foto.getEsPrincipal());

        marcarBorrada(foto);
        if (eraPrincipal) {
            // Ya sin la borrada (filtra deleted_at), la primera de la lista pasa a ser la principal
            fotoRepository.flush();
            fotoRepository.listar(idProducto).stream().findFirst().ifPresent(a -> a.setEsPrincipal(true));
        }
        return fotosDe(idProducto);
    }

    // Borra todas las fotos de un producto (al eliminar el producto)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminarTodas(Long idProducto) {
        fotoRepository.listar(idProducto).forEach(this::marcarBorrada);
    }

    // --- LÓGICA INTERNA ---

    // Marca la fila como borrada y programa la eliminación de los archivos para cuando la transacción se confirme
    private void marcarBorrada(Archivo foto) {
        foto.setDeletedAt(OffsetDateTime.now());
        foto.setEstatus("inactivo");
        foto.setEsPrincipal(false);
        String ruta = foto.getRutaRelativa() + "/" + foto.getNombreArchivo();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    almacenImagenes.eliminar(ruta);
                }
            });
        } else {
            almacenImagenes.eliminar(ruta);
        }
    }

    // Fotos del producto ya convertidas a su respuesta
    private List<FotoProductoResponseDTO> fotosDe(Long idProducto) {
        fotoRepository.flush();
        return RepositoryExecutor.execute(() -> fotoRepository.listar(idProducto), "Archivo", "listar").stream()
                .map(this::aRespuesta)
                .toList();
    }

    // Convierte la fila a su respuesta con la ruta de la foto y la de su miniatura
    private FotoProductoResponseDTO aRespuesta(Archivo a) {
        String ruta = a.getRutaRelativa() + "/" + a.getNombreArchivo();
        return FotoProductoResponseDTO.builder()
                .uuidFoto(a.getUuidArchivo())
                .rutaImagen(PREFIJO_VER + ruta)
                .rutaMiniatura(PREFIJO_VER + miniaturaDe(ruta))
                .esPrincipal(a.getEsPrincipal())
                .orden(a.getOrden())
                .build();
    }

    // La miniatura vive junto a la foto con el sufijo _m: abc.jpg → abc_m.jpg
    private String miniaturaDe(String ruta) {
        return ruta.endsWith(".jpg")
                ? ruta.substring(0, ruta.length() - 4) + AlmacenImagenes.SUFIJO_MINIATURA + ".jpg"
                : ruta;
    }

    // --- BÚSQUEDAS ---

    private Producto buscarProducto(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> productoRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Producto", uuid)),
                "Producto", "consultar");
    }

    private Archivo buscarFoto(Long idProducto, UUID uuidFoto) {
        return RepositoryExecutor.execute(
                () -> fotoRepository.buscarPorUuid(idProducto, uuidFoto)
                        .orElseThrow(() -> ErrorFactory.notFound("Foto", uuidFoto)),
                "Archivo", "consultar");
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
