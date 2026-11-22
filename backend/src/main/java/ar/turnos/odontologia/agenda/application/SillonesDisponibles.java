package ar.turnos.odontologia.agenda.application;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: sillones que el servidor puede asignar a un sobreturno, en orden determinista por id.
 * El cliente nunca elige el sillón (D1).
 */
public interface SillonesDisponibles {

    /** Sillones activos, ordenados por id ascendente. */
    List<Long> activos();

    /**
     * Enganche para la preferencia del sillón habitual (D2, Q1). C-04/V2 todavía no expone ese campo:
     * mientras tanto no hay preferencia y se usa el orden por id.
     * TODO(C-04/V2): devolver el sillón habitual del profesional cuando esté libre con margen.
     */
    default Optional<Long> preferidoPara(Long profesionalId) {
        return Optional.empty();
    }
}
