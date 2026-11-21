package ar.turnos.odontologia.agenda.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Turno de la agenda. Ocupa el intervalo {@code [inicio, fin)} de un profesional y un sillón.
 */
public record Turno(
        UUID id,
        UUID pacienteId,
        UUID profesionalId,
        UUID sillonId,
        UUID prestacionId,
        Intervalo intervalo,
        EstadoTurno estado) {

    public Turno {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(intervalo, "intervalo");
        Objects.requireNonNull(estado, "estado");
    }

    /** Crea un turno nuevo en estado {@link EstadoTurno#RESERVADO}. */
    public static Turno reservar(UUID pacienteId, UUID profesionalId, UUID sillonId,
            UUID prestacionId, Intervalo intervalo) {
        return new Turno(UUID.randomUUID(), pacienteId, profesionalId, sillonId, prestacionId,
                intervalo, EstadoTurno.RESERVADO);
    }

    public boolean esActivo() {
        return estado.esActivo();
    }

    public boolean mismoProfesionalQue(Turno otro) {
        return profesionalId.equals(otro.profesionalId);
    }

    public boolean mismoSillonQue(Turno otro) {
        return sillonId.equals(otro.sillonId);
    }
}
