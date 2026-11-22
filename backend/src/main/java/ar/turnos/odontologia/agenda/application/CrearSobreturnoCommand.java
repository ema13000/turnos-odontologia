package ar.turnos.odontologia.agenda.application;

import java.time.Instant;
import java.util.Objects;

/**
 * Datos para crear un sobreturno. No incluye {@code sillonId}: el servidor lo elige (D1). El motivo es
 * administrativo y se valida en el caso de uso para responder {@code DATOS_INVALIDOS}.
 */
public record CrearSobreturnoCommand(
        Long pacienteId,
        Long profesionalId,
        Long prestacionId,
        Instant inicio,
        String motivo) {

    public CrearSobreturnoCommand {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(inicio, "inicio");
    }
}
