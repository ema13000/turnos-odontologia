package ar.turnos.odontologia.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Reprogramación: nuevo inicio y, opcionalmente, otro profesional o sillón (nulos = los del original). */
record ReprogramarTurnoRequest(
        @NotBlank String dni,
        @NotBlank String numeroReserva,
        @NotNull OffsetDateTime inicio,
        Long profesionalId,
        Long sillonId) {}
