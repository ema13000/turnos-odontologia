package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Turno;
import java.util.Objects;

/**
 * Caso de uso: el personal cancela un turno activo en cualquier momento, después de validar el DNI del
 * paciente y el número de reserva (RN-AG-07). El horario queda libre de inmediato (RN-AG-08).
 */
public class CancelarTurno {

    private final TurnoRepository turnos;

    public CancelarTurno(TurnoRepository turnos) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
    }

    public Turno ejecutar(CancelarTurnoCommand comando) {
        Turno turno = IdentificacionReserva.turnoActivo(turnos, comando.turnoId(), comando.dni(),
                comando.numeroReserva());
        return turnos.guardar(turno.cancelado());
    }
}
