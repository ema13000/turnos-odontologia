package ar.turnos.odontologia.agenda.domain;

public enum EstadoTurno {
    RESERVADO,
    CONFIRMADO,
    CANCELADO,
    ATENDIDO,
    AUSENTE;

    /** Solo los turnos activos ocupan la agenda. */
    public boolean esActivo() {
        return this == RESERVADO || this == CONFIRMADO;
    }
}
