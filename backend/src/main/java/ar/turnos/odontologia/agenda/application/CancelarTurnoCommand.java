package ar.turnos.odontologia.agenda.application;

import java.util.Objects;

/** Datos para cancelar un turno: el DNI del paciente y el número de reserva lo identifican. */
public record CancelarTurnoCommand(Long turnoId, String dni, String numeroReserva) {

    public CancelarTurnoCommand {
        Objects.requireNonNull(turnoId, "turnoId");
    }
}
