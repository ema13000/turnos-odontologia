package ar.turnos.odontologia.agenda.domain.reglas;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Franja semanal de trabajo de un profesional: un día de la semana y el rango semiabierto {@code [desde, hasta)} en
 * hora local (RN-AG-13, design D4). Ninguna franja cruza la medianoche.
 */
public record FranjaHoraria(DayOfWeek dia, LocalTime desde, LocalTime hasta) {

    public FranjaHoraria {
        Objects.requireNonNull(dia, "dia");
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new DisponibilidadException(CodigoDisponibilidad.HORARIO_INVALIDO,
                    "La hora de fin de la franja tiene que ser posterior a la de inicio");
        }
    }

    /** @param dia día de la semana ISO-8601: 1 = lunes … 7 = domingo */
    public static FranjaHoraria de(int dia, LocalTime desde, LocalTime hasta) {
        if (dia < 1 || dia > 7) {
            throw new DisponibilidadException(CodigoDisponibilidad.HORARIO_INVALIDO,
                    "El día de la semana va de 1 (lunes) a 7 (domingo)");
        }
        return new FranjaHoraria(DayOfWeek.of(dia), desde, hasta);
    }

    /** Verdadero si el rango local {@code [inicio, fin)} entra completo en la franja, del mismo día. */
    public boolean contiene(DayOfWeek diaDelTurno, LocalTime inicio, LocalTime fin) {
        return dia == diaDelTurno && !inicio.isBefore(desde) && !fin.isAfter(hasta);
    }

    /** Dos franjas del mismo día que comparten algún instante. Las contiguas no se superponen. */
    public boolean seSuperponeCon(FranjaHoraria otra) {
        return dia == otra.dia && desde.isBefore(otra.hasta) && otra.desde.isBefore(hasta);
    }
}
