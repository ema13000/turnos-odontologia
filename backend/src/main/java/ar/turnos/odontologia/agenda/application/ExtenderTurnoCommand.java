package ar.turnos.odontologia.agenda.application;

import java.time.Instant;
import java.util.Objects;

/** Datos para extender un turno: id del turno, fin absoluto y motivo administrativo (RN-AG-14). */
public record ExtenderTurnoCommand(Long turnoId, Instant fin, String motivo) {

    public ExtenderTurnoCommand {
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(fin, "fin");
        Objects.requireNonNull(motivo, "motivo");
    }
}
