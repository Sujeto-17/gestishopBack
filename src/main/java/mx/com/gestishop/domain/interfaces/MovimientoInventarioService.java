package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.AjusteInventarioRequestDTO;
import mx.com.gestishop.application.dto.request.CostoMovimientoRequestDTO;
import mx.com.gestishop.application.dto.request.ResurtidoRequestDTO;
import mx.com.gestishop.application.dto.request.SalidaInventarioRequestDTO;
import mx.com.gestishop.application.dto.response.InventarioOpcionesResponseDTO;
import mx.com.gestishop.application.dto.response.MovimientoResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Movimientos de inventario: la única forma de cambiar la existencia de una variante
public interface MovimientoInventarioService {

    // Variantes activas con su existencia y proveedores activos (solo nombre) para los selectores; nunca incluye costos
    InventarioOpcionesResponseDTO opciones(Long idNegocio);

    // Entrada de mercancía por compra: suma a la existencia. Admin y gerente; el costo solo lo captura el admin
    MovimientoResponseDTO resurtir(ResurtidoRequestDTO dto, Long idNegocio, UUID uuidUsuario, boolean esAdmin);

    // Merma o consumo interno: resta de la existencia. Admin y gerente
    MovimientoResponseDTO registrarSalida(SalidaInventarioRequestDTO dto, Long idNegocio, UUID uuidUsuario);

    // Corrección por conteo físico (sube o baja la existencia). Solo admin
    MovimientoResponseDTO ajustar(AjusteInventarioRequestDTO dto, Long idNegocio, UUID uuidUsuario);

    // Historial del negocio con filtros opcionales (variante, tipo, motivo y rango de fechas, ambos días incluidos). El gerente solo ve compras, mermas y consumos, sin costos
    List<MovimientoResponseDTO> listar(UUID uuidVariante, String tipo, String motivo, LocalDate desde, LocalDate hasta, int limite,
                                       Long idNegocio, boolean esAdmin);

    // Resurtidos sin costo que el admin aún debe completar
    List<MovimientoResponseDTO> pendientesDeCosto(Long idNegocio);

    // El admin completa el costo de un resurtido; también actualiza el costo de la variante con ese proveedor
    MovimientoResponseDTO completarCosto(UUID uuidMovimiento, CostoMovimientoRequestDTO dto, Long idNegocio);
}
