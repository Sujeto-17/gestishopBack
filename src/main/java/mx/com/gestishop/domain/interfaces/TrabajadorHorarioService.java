package mx.com.gestishop.domain.interfaces;

import mx.com.gestishop.application.dto.request.AsignarHorarioRequestDTO;
import mx.com.gestishop.application.dto.response.HorarioVigenteTrabajadorResponseDTO;
import mx.com.gestishop.application.dto.response.TrabajadorHorarioResponseDTO;
import mx.com.gestishop.domain.model.Trabajador;

import java.util.List;
import java.util.UUID;

// Asignación de horarios a trabajadores y consulta de su historial
public interface TrabajadorHorarioService {

    // Cambia el horario de un trabajador: cierra el vigente y abre el nuevo en la fecha indicada
    TrabajadorHorarioResponseDTO asignar(UUID uuidUsuario, AsignarHorarioRequestDTO dto, Long idNegocio);

    // Primer horario al dar de alta al trabajador (lo llama TrabajadorServiceImpl dentro de su misma transacción)
    void asignarInicial(Trabajador trabajador, AsignarHorarioRequestDTO dto);

    // Horario que rige hoy para cada trabajador del negocio (los que no tienen ninguno vigente no aparecen)
    List<HorarioVigenteTrabajadorResponseDTO> vigentesDelNegocio(Long idNegocio);

    // Historial completo, del más reciente al más antiguo
    List<TrabajadorHorarioResponseDTO> historial(UUID uuidUsuario, Long idNegocio);
}

