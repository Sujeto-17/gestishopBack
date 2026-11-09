package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.ProveedorContactoRequestDTO;
import mx.com.gestishop.application.dto.request.ProveedorDescuentoRequestDTO;
import mx.com.gestishop.application.dto.request.ProveedorRequestDTO;
import mx.com.gestishop.application.dto.response.ProveedorResponseDTO;
import mx.com.gestishop.application.mapper.ProveedorMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.ProveedorService;
import mx.com.gestishop.domain.model.*;
import mx.com.gestishop.domain.repository.CatMunicipioRepository;
import mx.com.gestishop.domain.repository.NegocioRepository;
import mx.com.gestishop.domain.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Reglas del módulo Proveedores:
 *  - El nombre no se repite dentro del negocio (sin distinguir mayúsculas).
 *  - Cada contacto lleva al menos teléfono, WhatsApp o correo; solo uno puede ser el principal
 *    (si hay contactos y ninguno se marcó, el primero queda como principal).
 *  - Descuentos: tipo mensual (compra mensual acumulada), por_compra (monto de una compra) o fijo (siempre aplica).
 *    Mensual y por_compra llevan monto, no lo repiten y su porcentaje crece junto con el monto (cada tipo por separado);
 *    el fijo no lleva monto y solo puede haber uno.
 *  - Los costos de envío solo se guardan si el proveedor hace envíos.
 *  - Solo se elimina un proveedor que nunca se usó; en otro caso se desactiva para conservar el historial.
 * La BD aplica las mismas reglas y el aislamiento entre negocios como última defensa.
 */
@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private static final String TIPO_FIJO = "fijo";
    private static final Long SIN_EXCLUIR = -1L;   // al crear no hay proveedor que excluir en las comparaciones

    private final ProveedorRepository proveedorRepository;
    private final NegocioRepository negocioRepository;
    private final CatMunicipioRepository catMunicipioRepository;

    // --- CONSULTAS ---

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponseDTO> listar(Long idNegocio, boolean soloActivos) {
        Map<Long, Integer> variantes = totalesVariantes(idNegocio);
        return RepositoryExecutor.execute(
                        () -> proveedorRepository.listarPorNegocio(idNegocio), "Proveedor", "listar")
                .stream()
                .filter(p -> !soloActivos || "activo".equals(p.getEstatus()))
                .map(p -> ProveedorMapper.toResponse(p, variantes.getOrDefault(p.getIdProveedor(), 0), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponseDTO obtener(UUID uuidProveedor, Long idNegocio) {
        return aResponse(buscar(uuidProveedor, idNegocio), idNegocio);
    }

    // --- ESCRITURA ---

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProveedorResponseDTO crear(ProveedorRequestDTO dto, Long idNegocio) {
        validar(dto, idNegocio, SIN_EXCLUIR);

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findById(idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", idNegocio)),
                "Negocio", "consultar");

        Proveedor nuevo = new Proveedor();
        nuevo.setNegocio(negocio);
        aplicarDatos(nuevo, dto);
        aplicarContactos(nuevo, dto.getContactos());
        aplicarDescuentos(nuevo, dto.getDescuentos());

        Proveedor guardado = RepositoryExecutor.execute(
                () -> proveedorRepository.save(nuevo), "Proveedor", "crear");
        return aResponse(guardado, idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProveedorResponseDTO actualizar(UUID uuidProveedor, ProveedorRequestDTO dto, Long idNegocio) {
        Proveedor proveedor = buscar(uuidProveedor, idNegocio);
        validar(dto, idNegocio, proveedor.getIdProveedor());

        aplicarDatos(proveedor, dto);

        // Contactos y descuentos se reemplazan completos. Primero se vacían y se envía el borrado a la BD:
        // así los índices únicos (contacto principal, monto de descuento) no chocan con las filas anteriores.
        proveedor.getContactos().clear();
        proveedor.getDescuentos().clear();
        RepositoryExecutor.execute(() -> proveedorRepository.saveAndFlush(proveedor), "Proveedor", "limpiarHijos");

        aplicarContactos(proveedor, dto.getContactos());
        aplicarDescuentos(proveedor, dto.getDescuentos());

        Proveedor guardado = RepositoryExecutor.execute(
                () -> proveedorRepository.save(proveedor), "Proveedor", "actualizar");
        return aResponse(guardado, idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProveedorResponseDTO cambiarEstatus(UUID uuidProveedor, Long idNegocio) {
        Proveedor proveedor = buscar(uuidProveedor, idNegocio);
        proveedor.setEstatus("activo".equals(proveedor.getEstatus()) ? "inactivo" : "activo");

        Proveedor guardado = RepositoryExecutor.execute(
                () -> proveedorRepository.save(proveedor), "Proveedor", "cambiarEstatus");
        return aResponse(guardado, idNegocio);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void eliminar(UUID uuidProveedor, Long idNegocio) {
        Proveedor proveedor = buscar(uuidProveedor, idNegocio);

        long referencias = RepositoryExecutor.execute(
                () -> proveedorRepository.contarReferencias(proveedor.getIdProveedor()), "Proveedor", "validarUso");
        if (referencias > 0) {
            throw conflicto("No se puede eliminar: el proveedor ya tiene productos, compras o entradas registradas. "
                    + "Desactívalo para que deje de aparecer en las listas.");
        }
        RepositoryExecutor.executeVoid(() -> proveedorRepository.delete(proveedor), "Proveedor", "eliminar");
    }

    // --- VALIDACIONES ---

    // Reglas que no se pueden expresar con anotaciones: nombre libre, municipio, contactos y descuentos
    private void validar(ProveedorRequestDTO dto, Long idNegocio, Long excluirId) {
        boolean repetido = RepositoryExecutor.execute(
                () -> proveedorRepository.existeNombre(idNegocio, dto.getNombre(), excluirId), "Proveedor", "validarNombre");
        if (repetido) {
            throw conflicto("Ya tienes un proveedor llamado \"" + dto.getNombre() + "\".");
        }

        int principales = 0;
        for (ProveedorContactoRequestDTO c : dto.getContactos()) {
            if (c.getTelefono() == null && c.getWhatsapp() == null && c.getCorreo() == null) {
                throw invalido("El contacto \"" + c.getNombre() + "\" necesita al menos teléfono, WhatsApp o correo.");
            }
            if (Boolean.TRUE.equals(c.getEsPrincipal())) principales++;
        }
        if (principales > 1) {
            throw invalido("Solo un contacto puede ser el principal.");
        }

        validarDescuentos(dto.getDescuentos());
    }

    // Reglas de los descuentos: monto según el tipo, un solo fijo, sin montos repetidos y porcentaje creciente por tipo
    private void validarDescuentos(List<ProveedorDescuentoRequestDTO> descuentos) {
        long fijos = descuentos.stream().filter(d -> TIPO_FIJO.equals(d.getTipo())).count();
        if (fijos > 1) {
            throw invalido("Solo puede haber un descuento fijo.");
        }
        for (ProveedorDescuentoRequestDTO d : descuentos) {
            if (!TIPO_FIJO.equals(d.getTipo()) && d.getMontoMinimo() == null) {
                throw invalido("Los descuentos por compra mensual o por monto de una compra necesitan un monto.");
            }
        }

        // Cada tipo con monto se revisa por separado, ordenado por monto
        for (String tipo : List.of("mensual", "por_compra")) {
            List<ProveedorDescuentoRequestDTO> escalones = descuentos.stream()
                    .filter(d -> tipo.equals(d.getTipo()))
                    .sorted(Comparator.comparing(ProveedorDescuentoRequestDTO::getMontoMinimo))
                    .toList();
            for (int i = 1; i < escalones.size(); i++) {
                ProveedorDescuentoRequestDTO previo = escalones.get(i - 1);
                ProveedorDescuentoRequestDTO actual = escalones.get(i);
                if (actual.getMontoMinimo().compareTo(previo.getMontoMinimo()) == 0) {
                    throw invalido("Hay dos descuentos del mismo tipo con el mismo monto.");
                }
                if (actual.getPorcentaje().compareTo(previo.getPorcentaje()) <= 0) {
                    throw invalido("Dentro de un mismo tipo, un monto mayor debe tener un descuento mayor.");
                }
            }
        }
    }

    // --- APLICAR DATOS A LA ENTIDAD ---

    // Copia los datos propios del proveedor (no toca contactos ni descuentos)
    private void aplicarDatos(Proveedor p, ProveedorRequestDTO dto) {
        p.setNombre(dto.getNombre());
        p.setRazonSocial(dto.getRazonSocial());
        p.setRfc(dto.getRfc());
        p.setResumenSurtido(dto.getResumenSurtido());
        p.setFactura(dto.getFactura());
        p.setPreciosIncluyenIva(dto.getPreciosIncluyenIva());

        // Los datos de envío solo tienen sentido si el proveedor envía
        p.setHaceEnvios(dto.getHaceEnvios());
        p.setCostoEnvio(dto.getHaceEnvios() ? dto.getCostoEnvio() : null);
        p.setEnvioGratisDesde(dto.getHaceEnvios() ? dto.getEnvioGratisDesde() : null);

        p.setPedidoMinimo(dto.getPedidoMinimo());
        p.setFormasPago(dto.getFormasPago());
        p.setCalificacion(dto.getCalificacion());
        p.setDireccion(dto.getDireccion());
        p.setMunicipio(cargarMunicipio(dto.getIdMunicipio()));
        p.setSitioWeb(dto.getSitioWeb());
        p.setNotas(dto.getNotas());
    }

    // Agrega los contactos recibidos; si hay contactos y ninguno es principal, el primero lo será
    private void aplicarContactos(Proveedor p, List<ProveedorContactoRequestDTO> contactos) {
        boolean hayPrincipal = contactos.stream().anyMatch(c -> Boolean.TRUE.equals(c.getEsPrincipal()));
        for (int i = 0; i < contactos.size(); i++) {
            ProveedorContactoRequestDTO c = contactos.get(i);
            p.getContactos().add(ProveedorContacto.builder()
                    .proveedor(p)
                    .nombre(c.getNombre())
                    .puesto(c.getPuesto())
                    .telefono(c.getTelefono())
                    .whatsapp(c.getWhatsapp())
                    .correo(c.getCorreo())
                    .esPrincipal(Boolean.TRUE.equals(c.getEsPrincipal()) || (!hayPrincipal && i == 0))
                    .notas(c.getNotas())
                    .build());
        }
    }

    // Agrega los escalones de descuento recibidos
    private void aplicarDescuentos(Proveedor p, List<ProveedorDescuentoRequestDTO> descuentos) {
        for (ProveedorDescuentoRequestDTO d : descuentos) {
            p.getDescuentos().add(ProveedorDescuento.builder()
                    .proveedor(p)
                    .tipo(d.getTipo())
                    // el descuento fijo no lleva monto
                    .montoMinimo(TIPO_FIJO.equals(d.getTipo()) ? null : d.getMontoMinimo())
                    .porcentaje(d.getPorcentaje())
                    .descripcion(d.getDescripcion())
                    .build());
        }
    }

    // --- AUXILIARES ---

    private Proveedor buscar(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> proveedorRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Proveedor", uuid)),
                "Proveedor", "consultar");
    }

    // El municipio es opcional; si se indica debe existir
    private CatMunicipio cargarMunicipio(Long idMunicipio) {
        if (idMunicipio == null) return null;
        return RepositoryExecutor.execute(
                () -> catMunicipioRepository.findById(idMunicipio)
                        .orElseThrow(() -> ErrorFactory.notFound("CatMunicipio", idMunicipio)),
                "CatMunicipio", "consultar");
    }

    // Cantidad de variantes que surte cada proveedor: idProveedor -> total
    private Map<Long, Integer> totalesVariantes(Long idNegocio) {
        Map<Long, Integer> totales = new HashMap<>();
        for (Object[] fila : RepositoryExecutor.execute(
                () -> proveedorRepository.contarVariantesPorProveedor(idNegocio), "Proveedor", "contarVariantes")) {
            totales.put(((Number) fila[0]).longValue(), ((Number) fila[1]).intValue());
        }
        return totales;
    }

    private ProveedorResponseDTO aResponse(Proveedor p, Long idNegocio) {
        return ProveedorMapper.toResponse(p, totalesVariantes(idNegocio).getOrDefault(p.getIdProveedor(), 0), true);
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }

    private ApiResponseException invalido(String detalle) {
        return new ApiResponseException(ApiCodeResponse.REQUIRED_DATA, Map.of("detalle", detalle));
    }
}
