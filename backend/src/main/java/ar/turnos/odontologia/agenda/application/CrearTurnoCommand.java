package ar.turnos.odontologia.agenda.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Datos para crear un turno. El fin no se recibe: se calcula con la duración de la prestación. */
public record CrearTurnoCommand(
        UUID pacienteId,
        UUID profesionalId,
        UUID sillonId,
        UUID prestacionId,
        Instant inicio) {

    public CrearTurnoCommand {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(inicio, "inicio");
    }
}
