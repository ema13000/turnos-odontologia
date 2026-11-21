package ar.turnos.odontologia.agenda.application;

import java.time.Instant;
import java.util.Objects;

/**
 * Datos para reprogramar un turno. Si {@code profesionalId} o {@code sillonId} son nulos, el turno nuevo
 * conserva los del original.
 */
public record ReprogramarTurnoCommand(
        Long turnoId,
        String dni,
        String numeroReserva,
        Instant inicio,
        Long profesionalId,
        Long sillonId) {

    public ReprogramarTurnoCommand {
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(inicio, "inicio");
    }
}
