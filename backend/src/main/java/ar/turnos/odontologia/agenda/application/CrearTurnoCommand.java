package ar.turnos.odontologia.agenda.application;

import java.time.Instant;
import java.util.Objects;

/** Datos para crear un turno. El fin no se recibe: se calcula con la duración de la prestación. */
public record CrearTurnoCommand(
        Long pacienteId,
        Long profesionalId,
        Long sillonId,
        Long prestacionId,
        Instant inicio) {

    public CrearTurnoCommand {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(inicio, "inicio");
    }
}
