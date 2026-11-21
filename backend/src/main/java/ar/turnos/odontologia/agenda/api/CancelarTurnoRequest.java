package ar.turnos.odontologia.agenda.api;

import jakarta.validation.constraints.NotBlank;

/** El personal identifica el turno con el DNI del paciente y el número de reserva (RN-AG-07). */
record CancelarTurnoRequest(
        @NotBlank String dni,
        @NotBlank String numeroReserva) {}
