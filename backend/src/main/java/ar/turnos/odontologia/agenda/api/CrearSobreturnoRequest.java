package ar.turnos.odontologia.agenda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Pedido de creación de un sobreturno. No incluye el identificador de sillón: lo asigna el servidor (D1). El motivo es
 * administrativo: se valida acá y nunca se devuelve ni se registra en la auditoría.
 */
record CrearSobreturnoRequest(
        @NotNull Long pacienteId,
        @NotNull Long profesionalId,
        @NotNull Long prestacionId,
        @NotNull OffsetDateTime inicio,
        @NotBlank @Size(max = 500) String motivo) {}
