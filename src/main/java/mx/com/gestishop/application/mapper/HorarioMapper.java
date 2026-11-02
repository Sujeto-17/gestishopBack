package mx.com.gestishop.application.mapper;

import mx.com.gestishop.application.dto.request.HorarioDiaRequestDTO;
import mx.com.gestishop.application.dto.request.HorarioRequestDTO;
import mx.com.gestishop.application.dto.response.*;
import mx.com.gestishop.domain.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Convierte entre las entidades de horarios y sus DTOs. También arma el resumen legible
 * ("L-V 08:00–16:00 · S 08:00–13:00") y la "firma" de un horario, que sirve para detectar horarios repetidos.
 */
public class HorarioMapper {

    private static final String[] DIAS = {"", "L", "M", "Mi", "J", "V", "S", "D"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private HorarioMapper() {
    }

    // Construye un horario propio del negocio, con sus días ya enlazados. El negocio lo asigna el service
    public static Horario toEntity(HorarioRequestDTO dto, Negocio negocio) {
        Horario horario = Horario.builder()
                .negocio(negocio)
                .nombre(dto.getNombre().trim())
                .estatus("activo")
                .build();
        dto.getDias().forEach(d -> horario.agregarDia(toDia(d)));
        return horario;
    }

    /**
     * Aplica los cambios de una edición. Los días se sincronizan uno a uno: se actualizan los que siguen,
     * se quitan los que ya no están y se agregan los nuevos. Así nunca se borra y reinserta el mismo día
     * dentro del mismo guardado (lo que chocaría con la restricción única de horario + día).
     */
    public static void actualizarEntidad(Horario horario, HorarioRequestDTO dto) {
        horario.setNombre(dto.getNombre().trim());

        Map<Short, HorarioDiaRequestDTO> nuevos = dto.getDias().stream()
                .collect(Collectors.toMap(d -> d.getDiaSemana().shortValue(), d -> d));

        horario.getDias().removeIf(existente -> !nuevos.containsKey(existente.getDiaSemana()));

        nuevos.forEach((dia, req) -> horario.getDias().stream()
                .filter(existente -> existente.getDiaSemana().equals(dia))
                .findFirst()
                .ifPresentOrElse(
                        existente -> {
                            existente.setHoraEntrada(req.getHoraEntrada());
                            existente.setHoraSalida(req.getHoraSalida());
                        },
                        () -> horario.agregarDia(toDia(req))));
    }

    private static HorarioDia toDia(HorarioDiaRequestDTO dto) {
        return HorarioDia.builder()
                .diaSemana(dto.getDiaSemana().shortValue())
                .horaEntrada(dto.getHoraEntrada())
                .horaSalida(dto.getHoraSalida())
                .build();
    }

    // Respuesta de un horario; usado y vigentes los calcula el service con consultas al historial
    public static HorarioResponseDTO toResponse(Horario h, boolean usado, int trabajadoresVigentes) {
        return HorarioResponseDTO.builder()
                .uuidHorario(h.getUuidHorario())
                .nombre(h.getNombre())
                .estatus(h.getEstatus())
                .resumen(resumen(h.getDias()))
                .dias(toDiasResponse(h.getDias()))
                .global(h.esGlobal())
                .usado(usado)
                .editable(!h.esGlobal() && !usado)
                .trabajadoresVigentes(trabajadoresVigentes)
                .build();
    }

    public static List<HorarioDiaResponseDTO> toDiasResponse(Collection<HorarioDia> dias) {
        return dias.stream()
                .sorted(Comparator.comparing(HorarioDia::getDiaSemana))
                .map(d -> HorarioDiaResponseDTO.builder()
                        .diaSemana(d.getDiaSemana().intValue())
                        .horaEntrada(d.getHoraEntrada())
                        .horaSalida(d.getHoraSalida())
                        .cruzaMedianoche(d.cruzaMedianoche())
                        .build())
                .toList();
    }

    public static ToleranciaResponseDTO toResponse(CatTolerancia t) {
        return ToleranciaResponseDTO.builder()
                .idTolerancia(t.getIdTolerancia())
                .minutos(t.getMinutos().intValue())
                .nombre(t.getNombre())
                .build();
    }

    // Renglón del historial; vigente/programado se calculan contra la fecha "de hoy" que manda el service
    public static TrabajadorHorarioResponseDTO toResponse(TrabajadorHorario th, LocalDate hoy) {
        Horario h = th.getHorario();
        boolean empezo = !th.getVigenteDesde().isAfter(hoy);
        boolean noTermino = th.getVigenteHasta() == null || !th.getVigenteHasta().isBefore(hoy);

        return TrabajadorHorarioResponseDTO.builder()
                .uuidHorario(h.getUuidHorario())
                .nombreHorario(h.getNombre())
                .resumen(resumen(h.getDias()))
                .dias(toDiasResponse(h.getDias()))
                .vigenteDesde(th.getVigenteDesde())
                .vigenteHasta(th.getVigenteHasta())
                .vigente(empezo && noTermino)
                .programado(!empezo)
                .build();
    }

    // Horario vigente de un trabajador, en versión corta para el listado
    public static HorarioVigenteTrabajadorResponseDTO toVigente(TrabajadorHorario th) {
        Horario h = th.getHorario();
        return HorarioVigenteTrabajadorResponseDTO.builder()
                .uuidUsuario(th.getTrabajador().getUsuario().getUuidUsuario())
                .uuidHorario(h.getUuidHorario())
                .nombreHorario(h.getNombre())
                .resumen(resumen(h.getDias()))
                .build();
    }

    /**
     * Texto corto del horario: agrupa los días que comparten las mismas horas y junta los consecutivos.
     * Ej.: L-V 08:00–16:00 · S 08:00–13:00
     */
    public static String resumen(Collection<HorarioDia> dias) {
        Map<String, List<Integer>> grupos = new LinkedHashMap<>();
        dias.stream()
                .sorted(Comparator.comparing(HorarioDia::getDiaSemana))
                .forEach(d -> grupos
                        .computeIfAbsent(HORA.format(d.getHoraEntrada()) + "–" + HORA.format(d.getHoraSalida()), k -> new ArrayList<>())
                        .add(d.getDiaSemana().intValue()));

        return grupos.entrySet().stream()
                .map(e -> rangosDeDias(e.getValue()) + " " + e.getKey())
                .collect(Collectors.joining(" · "));
    }

    // Une días consecutivos: [1,2,3,4,5,7] -> "L-V, D"; [1,2] -> "L, M"; [6] -> "S"
    private static String rangosDeDias(List<Integer> dias) {
        List<String> partes = new ArrayList<>();
        int i = 0;
        while (i < dias.size()) {
            int j = i;
            while (j + 1 < dias.size() && dias.get(j + 1) == dias.get(j) + 1) j++;
            int largo = j - i + 1;
            if (largo >= 3) partes.add(DIAS[dias.get(i)] + "-" + DIAS[dias.get(j)]);
            else for (int k = i; k <= j; k++) partes.add(DIAS[dias.get(k)]);
            i = j + 1;
        }
        return String.join(", ", partes);
    }

    // --- FIRMA: identifica un horario por sus días y horas, para detectar horarios repetidos ---

    public static String firmaDeEntidad(Collection<HorarioDia> dias) {
        return dias.stream()
                .sorted(Comparator.comparing(HorarioDia::getDiaSemana))
                .map(d -> firmaDia(d.getDiaSemana().intValue(), d.getHoraEntrada(), d.getHoraSalida()))
                .collect(Collectors.joining("|"));
    }

    public static String firmaDeRequest(Collection<HorarioDiaRequestDTO> dias) {
        return dias.stream()
                .sorted(Comparator.comparing(HorarioDiaRequestDTO::getDiaSemana))
                .map(d -> firmaDia(d.getDiaSemana(), d.getHoraEntrada(), d.getHoraSalida()))
                .collect(Collectors.joining("|"));
    }

    private static String firmaDia(int dia, LocalTime entrada, LocalTime salida) {
        return dia + "@" + HORA.format(entrada) + "-" + HORA.format(salida);
    }
}
