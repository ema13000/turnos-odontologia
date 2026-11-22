package ar.turnos.odontologia.agenda.application;

import java.time.Instant;
import java.util.Objects;

/**
 * Datos para reasignar un turno afectado por una ausencia (C-07, design D10). Si {@code profesionalId},
 * {@code inicio} o {@code sillonId} son nulos, el turno nuevo conserva los del original. No lleva DNI ni número de
 * reserva: la reasignación la inicia el consultorio.
 */
public record ReasignarTurnoCommand(Long turnoId, Long profesionalId, Instant inicio, Long sillonId) {

    public ReasignarTurnoCommand {
        Objects.requireNonNull(turnoId, "turnoId");
    }
}
