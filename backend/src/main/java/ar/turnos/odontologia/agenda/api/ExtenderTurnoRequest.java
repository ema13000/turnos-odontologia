package ar.turnos.odontologia.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Pedido de extensión: fin absoluto en ISO-8601 con zona horaria y motivo administrativo. */
record ExtenderTurnoRequest(
        @NotNull OffsetDateTime fin,
        @NotBlank @Size(max = 500) String motivo) {}
