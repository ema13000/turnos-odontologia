package ar.turnos.odontologia.agenda.domain;

import java.util.Objects;

/**
 * Turno de la agenda. Ocupa el intervalo {@code [inicio, fin)} de un profesional y un sillón.
 *
 * <p>Los identificadores son {@code BIGINT} en la base (DD-12). Un turno recién creado no tiene
 * {@code id} hasta que se guarda: lo asigna la base al insertarlo.
 */
public record Turno(
        Long id,
        Long pacienteId,
        Long profesionalId,
        Long sillonId,
        Long prestacionId,
        Intervalo intervalo,
        EstadoTurno estado) {

    public Turno {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(intervalo, "intervalo");
        Objects.requireNonNull(estado, "estado");
    }

    /** Crea un turno nuevo, todavía sin guardar, en estado {@link EstadoTurno#RESERVADO}. */
    public static Turno reservar(Long pacienteId, Long profesionalId, Long sillonId,
            Long prestacionId, Intervalo intervalo) {
        return new Turno(null, pacienteId, profesionalId, sillonId, prestacionId,
                intervalo, EstadoTurno.RESERVADO);
    }

    /** El mismo turno con el identificador que le asignó la base al guardarlo. */
    public Turno conId(Long nuevoId) {
        return new Turno(Objects.requireNonNull(nuevoId, "nuevoId"), pacienteId, profesionalId,
                sillonId, prestacionId, intervalo, estado);
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
