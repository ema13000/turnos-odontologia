package ar.turnos.odontologia.notificaciones.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Lo único que un mensaje sabe del turno: cuándo es. No tiene campo para el paciente, el profesional, la prestación,
 * el motivo de un sobreturno ni ningún dato clínico: lo que no existe en el dato de entrada no puede filtrarse
 * (RN-NO-01, regla dura 6).
 */
public record DatosDelTurno(Instant inicio) {

    public DatosDelTurno {
        Objects.requireNonNull(inicio, "inicio");
    }
}
