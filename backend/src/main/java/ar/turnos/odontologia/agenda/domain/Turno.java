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
        EstadoTurno estado,
        NumeroReserva numeroReserva) {

    public Turno {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(intervalo, "intervalo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(numeroReserva, "numeroReserva");
    }

    /** Crea un turno nuevo, todavía sin guardar, en estado {@link EstadoTurno#RESERVADO}. */
    public static Turno reservar(Long pacienteId, Long profesionalId, Long sillonId,
            Long prestacionId, Intervalo intervalo, NumeroReserva numeroReserva) {
        return new Turno(null, pacienteId, profesionalId, sillonId, prestacionId,
                intervalo, EstadoTurno.RESERVADO, numeroReserva);
    }

    /** El mismo turno con el identificador que le asignó la base al guardarlo. */
    public Turno conId(Long nuevoId) {
        return new Turno(Objects.requireNonNull(nuevoId, "nuevoId"), pacienteId, profesionalId,
                sillonId, prestacionId, intervalo, estado, numeroReserva);
    }

    /** El mismo turno en estado {@link EstadoTurno#CANCELADO}: deja de ocupar la agenda (RN-AG-08). */
    public Turno cancelado() {
        return new Turno(id, pacienteId, profesionalId, sillonId, prestacionId, intervalo,
                EstadoTurno.CANCELADO, numeroReserva);
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
