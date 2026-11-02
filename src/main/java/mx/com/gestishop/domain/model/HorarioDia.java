package mx.com.gestishop.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Un día de la semana dentro de un horario, con su hora de entrada y de salida.
 * Un día sin fila es día de descanso. Si la salida es menor que la entrada, el turno cruza
 * medianoche (ej. 22:00 a 06:00) y pertenece al día en que EMPIEZA.
 */
@Entity
@Table(name = "horario_dias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_dia")
    private Long idHorarioDia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario", nullable = false)
    private Horario horario;

    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;   // ISO: 1 = lunes ... 7 = domingo

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida;

    // true si la salida cae al día siguiente (la salida es anterior a la entrada)
    public boolean cruzaMedianoche() {
        return horaSalida.isBefore(horaEntrada);
    }
}
