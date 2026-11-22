package ar.turnos.odontologia.agenda.api;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Pedido de creación de un turno. El inicio viaja en ISO-8601 con zona horaria; el fin lo calcula la agenda. */
record CrearTurnoRequest(
        @NotNull Long pacienteId,
        @NotNull Long profesionalId,
        @NotNull Long sillonId,
        @NotNull Long prestacionId,
        @NotNull OffsetDateTime inicio) {}
