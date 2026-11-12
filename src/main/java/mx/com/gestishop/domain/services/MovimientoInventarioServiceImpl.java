package mx.com.gestishop.domain.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.AjusteInventarioRequestDTO;
import mx.com.gestishop.application.dto.request.CostoMovimientoRequestDTO;
import mx.com.gestishop.application.dto.request.ResurtidoRequestDTO;
import mx.com.gestishop.application.dto.request.SalidaInventarioRequestDTO;
import mx.com.gestishop.application.dto.response.InventarioOpcionesResponseDTO;
import mx.com.gestishop.application.dto.response.MovimientoResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.core.utils.DateUtils;
import mx.com.gestishop.domain.interfaces.MovimientoInventarioService;
import mx.com.gestishop.domain.interfaces.VarianteCostoService;
import mx.com.gestishop.domain.model.*;
import mx.com.gestishop.domain.repository.MovimientoInventarioRepository;
import mx.com.gestishop.domain.repository.ProveedorRepository;
import mx.com.gestishop.domain.repository.UsuarioRepository;
import mx.com.gestishop.domain.repository.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reglas de los movimientos de inventario:
 *  - El stock solo cambia con movimientos (un trigger de la BD lo aplica); aquí nunca se edita el número.
 *  - Resurtido: la cantidad que llegó se SUMA a la existencia y se indica a qué proveedor se le compró.
 *    El gerente no ve ni captura costos: si el admin no los pone, el resurtido queda pendiente de costo.
 *  - Merma y consumo: solo restan, exigen explicar qué pasó y no pueden dejar el stock en negativo.
 *  - Ajuste (conteo físico): solo admin; se indica la existencia real y se registra la diferencia.
 *  - Al completar un costo, también se actualiza el costo de la variante con ese proveedor (queda en el historial).
 */
@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private static final int LIMITE_MAXIMO = 500;
    private static final Set<String> TIPOS = Set.of("entrada", "salida", "ajuste");
    private static final Set<String> MOTIVOS = Set.of("compra", "recepcion_solicitud", "inventario_inicial", "venta",
            "consumo", "merma", "correccion", "devolucion", "otro");

    private final MovimientoInventarioRepository movimientoRepository;
    private final VarianteRepository varianteRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final VarianteCostoService costoService;

    @PersistenceContext
    private EntityManager entityManager;

    // --- OPCIONES ---

    // Variantes activas y proveedores activos para los selectores; nunca incluye costos
    @Override
    @Transactional(readOnly = true)
    public InventarioOpcionesResponseDTO opciones(Long idNegocio) {
        var variantes = RepositoryExecutor.execute(
                        () -> movimientoRepository.variantesActivas(idNegocio), "Variante", "consultar").stream()
                .map(r -> InventarioOpcionesResponseDTO.VarianteOpcion.builder()
                        .uuidVariante(UUID.fromString(String.valueOf(r[0])))
                        .producto((String) r[1])
                        .variante((String) r[2])
                        .existencia((BigDecimal) r[3])
                        .unidad((String) r[4])
                        .build())
                .toList();
        var proveedores = RepositoryExecutor.execute(
                        () -> movimientoRepository.proveedoresActivos(idNegocio), "Proveedor", "consultar").stream()
                .map(r -> InventarioOpcionesResponseDTO.ProveedorOpcion.builder()
                        .uuidProveedor(UUID.fromString(String.valueOf(r[0])))
                        .nombre((String) r[1])
                        .build())
                .toList();
        return InventarioOpcionesResponseDTO.builder().variantes(variantes).proveedores(proveedores).build();
    }

    // --- ESCRITURA ---

    // Entrada de mercancía por compra: suma a la existencia
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MovimientoResponseDTO resurtir(ResurtidoRequestDTO dto, Long idNegocio, UUID uuidUsuario, boolean esAdmin) {
        Variante variante = buscarVariante(dto.getUuidVariante(), idNegocio);
        if (!"activo".equals(variante.getEstatus())) {
            throw conflicto("La variante está inactiva: actívala para poder resurtirla.");
        }
        validarCantidad(dto.getCantidad(), variante.getProducto().getUnidad());
        if (dto.getCostoUnitario() != null && !esAdmin) {
            throw new ApiResponseException(ApiCodeResponse.FORBIDDEN,
                    Map.of("detalle", "Solo el administrador puede capturar costos."));
        }
        Proveedor proveedor = buscarProveedor(dto.getUuidProveedor(), idNegocio);
        if (!"activo".equals(proveedor.getEstatus())) {
            throw conflicto("El proveedor \"" + proveedor.getNombre() + "\" está inactivo.");
        }
        Usuario usuario = buscarUsuario(uuidUsuario);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .idNegocio(idNegocio)
                .variante(variante)
                .tipo("entrada")
                .motivo("compra")
                .cantidad(dto.getCantidad())
                .usuario(usuario)
                .proveedor(proveedor)
                .costoUnitario(dto.getCostoUnitario())
                .notas(dto.getNotas())
                .build();
        MovimientoInventario guardado = guardar(movimiento);

        // Con costo, la variante también registra lo que cuesta con este proveedor
        if (dto.getCostoUnitario() != null) {
            costoService.registrarCostoDeCompra(variante.getIdVariante(), dto.getUuidProveedor(), dto.getCostoUnitario(), idNegocio);
        }
        return recargar(guardado.getUuidMovimiento(), idNegocio, esAdmin);
    }

    // Merma o consumo interno: resta de la existencia
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MovimientoResponseDTO registrarSalida(SalidaInventarioRequestDTO dto, Long idNegocio, UUID uuidUsuario) {
        Variante variante = buscarVariante(dto.getUuidVariante(), idNegocio);
        validarCantidad(dto.getCantidad(), variante.getProducto().getUnidad());
        validarExistenciaSuficiente(variante, dto.getCantidad());
        Usuario usuario = buscarUsuario(uuidUsuario);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .idNegocio(idNegocio)
                .variante(variante)
                .tipo("salida")
                .motivo(dto.getMotivo())
                .cantidad(dto.getCantidad().negate())
                .usuario(usuario)
                .notas(dto.getNotas())
                .build();
        MovimientoInventario guardado = guardar(movimiento);
        return recargar(guardado.getUuidMovimiento(), idNegocio, false);
    }

    // Corrección por conteo físico: se registra la diferencia entre lo contado y lo que decía el sistema
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MovimientoResponseDTO ajustar(AjusteInventarioRequestDTO dto, Long idNegocio, UUID uuidUsuario) {
        Variante variante = buscarVariante(dto.getUuidVariante(), idNegocio);
        validarCantidad(dto.getExistenciaReal(), variante.getProducto().getUnidad());
        BigDecimal diferencia = dto.getExistenciaReal().subtract(variante.getStockActual());
        if (diferencia.signum() == 0) {
            throw conflicto("La existencia contada coincide con la del sistema: no hay nada que ajustar.");
        }
        Usuario usuario = buscarUsuario(uuidUsuario);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .idNegocio(idNegocio)
                .variante(variante)
                .tipo("ajuste")
                .motivo("correccion")
                .cantidad(diferencia)
                .usuario(usuario)
                .notas(dto.getNotas())
                .build();
        MovimientoInventario guardado = guardar(movimiento);
        return recargar(guardado.getUuidMovimiento(), idNegocio, true);
    }

    // El admin completa el costo de un resurtido pendiente y la variante aprende lo que cuesta con ese proveedor
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MovimientoResponseDTO completarCosto(UUID uuidMovimiento, CostoMovimientoRequestDTO dto, Long idNegocio) {
        MovimientoInventario movimiento = RepositoryExecutor.execute(
                () -> movimientoRepository.buscarPorUuid(uuidMovimiento, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Movimiento", uuidMovimiento)),
                "MovimientoInventario", "consultar");
        if (!"entrada".equals(movimiento.getTipo()) || !"compra".equals(movimiento.getMotivo()) || movimiento.getProveedor() == null) {
            throw conflicto("Solo se puede capturar el costo de un resurtido.");
        }
        if (movimiento.getCostoUnitario() != null) {
            throw conflicto("Este resurtido ya tiene costo y no se puede modificar.");
        }
        // Se guardan antes de la actualización, que vacía la caché de persistencia
        Long idMovimiento = movimiento.getIdMovimiento();
        Long idVariante = movimiento.getVariante().getIdVariante();
        UUID uuidProveedor = movimiento.getProveedor().getUuidProveedor();

        int cambiados = RepositoryExecutor.execute(
                () -> movimientoRepository.asignarCosto(idMovimiento, dto.getCostoUnitario()), "MovimientoInventario", "completarCosto");
        if (cambiados == 0) {
            throw conflicto("Este resurtido ya tiene costo y no se puede modificar.");
        }
        costoService.registrarCostoDeCompra(idVariante, uuidProveedor, dto.getCostoUnitario(), idNegocio);
        return recargar(uuidMovimiento, idNegocio, true);
    }

    // --- CONSULTAS ---

    // Historial con filtros opcionales; el gerente solo ve compras, mermas y consumos, sin costos
    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> listar(UUID uuidVariante, String tipo, String motivo, LocalDate desde, LocalDate hasta,
                                              int limite, Long idNegocio, boolean esAdmin) {
        if (tipo != null && !TIPOS.contains(tipo)) throw conflicto("Tipo de movimiento no válido: " + tipo);
        if (motivo != null && !MOTIVOS.contains(motivo)) throw conflicto("Motivo de movimiento no válido: " + motivo);
        Long idVariante = uuidVariante == null ? null : buscarVariante(uuidVariante, idNegocio).getIdVariante();
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw conflicto("La fecha final no puede ser anterior a la inicial.");
        }
        return consultar(idNegocio, idVariante, null, tipo, motivo, desde, hasta, false, !esAdmin, limite, esAdmin);
    }

    // Resurtidos sin costo que el admin aún debe completar
    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponseDTO> pendientesDeCosto(Long idNegocio) {
        return consultar(idNegocio, null, null, null, null, null, null, true, false, LIMITE_MAXIMO, true);
    }

    // --- LÓGICA INTERNA ---

    // Inserta el movimiento; el trigger de la BD aplica el stock y valida que no quede negativo
    private MovimientoInventario guardar(MovimientoInventario movimiento) {
        return RepositoryExecutor.execute(() -> movimientoRepository.save(movimiento), "MovimientoInventario", "crear");
    }

    // Lee de nuevo (sin caché) el movimiento para traer lo que llenó el trigger (stock anterior y resultante)
    private MovimientoResponseDTO recargar(UUID uuidMovimiento, Long idNegocio, boolean esAdmin) {
        entityManager.flush();
        entityManager.clear();
        return consultar(idNegocio, null, uuidMovimiento, null, null, null, null, false, false, 1, esAdmin).stream().findFirst()
                .orElseThrow(() -> ErrorFactory.notFound("Movimiento", uuidMovimiento));
    }

    // Ejecuta la consulta de movimientos y la convierte a respuesta; el costo solo se informa al admin
    private List<MovimientoResponseDTO> consultar(Long idNegocio, Long idVariante, UUID uuidMovimiento, String tipo, String motivo,
                                                  LocalDate desde, LocalDate hasta, boolean soloPendientes, boolean soloOperativos,
                                                  int limite, boolean esAdmin) {
        int tope = Math.min(Math.max(limite, 1), LIMITE_MAXIMO);
        // El día final se incluye completo: el rango real llega hasta el inicio del día siguiente
        ZoneId zona = ZoneId.systemDefault();
        OffsetDateTime desdeFecha = desde == null ? null : desde.atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime hastaFecha = hasta == null ? null : hasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime();
        return RepositoryExecutor.execute(
                        () -> movimientoRepository.consultar(idNegocio, idVariante,
                                uuidMovimiento == null ? null : uuidMovimiento.toString(),
                                tipo, motivo, desdeFecha, hastaFecha, soloPendientes, soloOperativos, tope),
                        "MovimientoInventario", "consultar")
                .stream()
                .map(r -> aRespuesta(r, esAdmin))
                .toList();
    }

    // Convierte una fila de la consulta nativa a su respuesta
    private MovimientoResponseDTO aRespuesta(Object[] r, boolean esAdmin) {
        String tipo = (String) r[2];
        String motivo = (String) r[3];
        BigDecimal costo = (BigDecimal) r[13];
        return MovimientoResponseDTO.builder()
                .uuidMovimiento(UUID.fromString(String.valueOf(r[0])))
                .fecha(DateUtils.aOffsetDateTime(r[1]))
                .tipo(tipo)
                .motivo(motivo)
                .cantidad((BigDecimal) r[4])
                .stockAnterior((BigDecimal) r[5])
                .stockResultante((BigDecimal) r[6])
                .uuidVariante(UUID.fromString(String.valueOf(r[7])))
                .producto((String) r[8])
                .variante((String) r[9])
                .unidad((String) r[10])
                .usuario((String) r[11])
                .proveedor((String) r[12])
                .costoUnitario(esAdmin ? costo : null)
                .pendienteCosto(esAdmin ? "entrada".equals(tipo) && "compra".equals(motivo) && costo == null : null)
                .notas((String) r[14])
                .build();
    }

    // Si la unidad no admite decimales (piezas), la cantidad debe ser entera
    private void validarCantidad(BigDecimal cantidad, CatUnidadMedida unidad) {
        boolean admiteDecimales = Boolean.TRUE.equals(unidad.getPermiteDecimales());
        if (!admiteDecimales && cantidad.stripTrailingZeros().scale() > 0) {
            throw conflicto("La cantidad debe ser un número entero: la unidad \"" + unidad.getNombre() + "\" no admite decimales.");
        }
    }

    // Evita restar más de lo que hay (el trigger de la BD también lo impide, pero así el mensaje es claro)
    private void validarExistenciaSuficiente(Variante variante, BigDecimal cantidad) {
        if (variante.getStockActual().compareTo(cantidad) < 0) {
            throw conflicto("Existencia insuficiente: hay " + variante.getStockActual().stripTrailingZeros().toPlainString()
                    + " y se intenta restar " + cantidad.stripTrailingZeros().toPlainString() + ".");
        }
    }

    // --- BÚSQUEDAS ---

    private Variante buscarVariante(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> varianteRepository.buscarPorUuidEnNegocio(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Variante", uuid)),
                "Variante", "consultar");
    }

    private Proveedor buscarProveedor(UUID uuid, Long idNegocio) {
        return RepositoryExecutor.execute(
                () -> proveedorRepository.buscarPorUuid(uuid, idNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Proveedor", uuid)),
                "Proveedor", "consultar");
    }

    private Usuario buscarUsuario(UUID uuidUsuario) {
        return RepositoryExecutor.execute(
                () -> usuarioRepository.buscarPorUuid(uuidUsuario)
                        .orElseThrow(() -> ErrorFactory.notFound("Usuario", uuidUsuario)),
                "Usuario", "consultar");
    }

    private ApiResponseException conflicto(String detalle) {
        return new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", detalle));
    }
}
