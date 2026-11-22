package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import java.util.Objects;

/**
 * Caso de uso (C-11): un turno {@code RESERVADO} pasa a {@code CONFIRMADO} cuando el paciente confirma su
 * asistencia. No cambia el horario ni los recursos, así que no vuelve a verificar solapamientos: el turno ya
 * ocupaba la agenda como activo.
 */
public class ConfirmarTurno {

    private final TurnoRepository turnos;

    public ConfirmarTurno(TurnoRepository turnos) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
    }

    /**
     * @throws TurnoInexistenteException si no existe el turno
     * @throws ar.turnos.odontologia.agenda.domain.TurnoRechazadoException si el turno no está {@code RESERVADO}
     */
    public Turno ejecutar(Long turnoId) {
        Turno turno = turnos.buscarParaModificar(turnoId)
                .orElseThrow(() -> new TurnoInexistenteException(turnoId))
                .turno();
        return turnos.guardar(turno.confirmado());
    }
}
