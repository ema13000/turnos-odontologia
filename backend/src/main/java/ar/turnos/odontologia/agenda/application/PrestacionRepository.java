package ar.turnos.odontologia.agenda.application;

import java.time.Duration;
import java.util.UUID;

/** Puerto de salida: duración de las prestaciones. La implementación real llega con C-04. */
public interface PrestacionRepository {

    /**
     * Devuelve la duración de la prestación.
     *
     * @throws IllegalArgumentException si la prestación no existe
     */
    Duration duracionDe(UUID prestacionId);
}
