package ar.turnos.odontologia.agenda.domain.reglas;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.ZoneId;

/**
 * Reglas de disponibilidad del profesional para un turno (C-07, design D3). Informa un solo rechazo, con esta
 * prioridad: ausencia o licencia (RN-AG-16), bloqueo (RN-AG-10) y fuera de horario (RN-AG-13). El sobreturno y la
 * extensión pueden quedar fuera de horario (RN-AG-06, RN-AG-14), pero no saltean ausencias ni bloqueos.
 */
public final class ReglasDisponibilidad {

    private ReglasDisponibilidad() {}

    /**
     * @param exigirHorario {@code false} para el sobreturno y la extensión
     * @throws TurnoRechazadoException con {@code PROFESIONAL_AUSENTE}, {@code AGENDA_BLOQUEADA} o
     *     {@code FUERA_DE_HORARIO}
     */
    public static void verificar(Intervalo intervalo, PeriodosProfesional periodos, ZoneId zona,
                                 boolean exigirHorario) {
        if (periodos.periodos().stream().anyMatch(p -> p.tipo().esAusencia() && p.seCruzaCon(intervalo))) {
            throw new TurnoRechazadoException(CodigoRechazo.PROFESIONAL_AUSENTE,
                    "El profesional tiene una ausencia o licencia en ese horario");
        }
        if (periodos.periodos().stream().anyMatch(p -> !p.tipo().esAusencia() && p.seCruzaCon(intervalo))) {
            throw new TurnoRechazadoException(CodigoRechazo.AGENDA_BLOQUEADA,
                    "La agenda del profesional está bloqueada en ese horario");
        }
        if (exigirHorario && !periodos.horario().contiene(intervalo, zona)) {
            throw new TurnoRechazadoException(CodigoRechazo.FUERA_DE_HORARIO,
                    "El turno no queda dentro de una franja de trabajo del profesional");
        }
    }
}
