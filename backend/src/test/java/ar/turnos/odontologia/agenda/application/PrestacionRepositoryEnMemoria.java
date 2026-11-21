package ar.turnos.odontologia.agenda.application;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Doble de prueba de {@link PrestacionRepository}. */
class PrestacionRepositoryEnMemoria implements PrestacionRepository {

    private final Map<UUID, Duration> duraciones = new HashMap<>();

    void agregar(UUID prestacionId, Duration duracion) {
        duraciones.put(prestacionId, duracion);
    }

    @Override
    public Duration duracionDe(UUID prestacionId) {
        Duration duracion = duraciones.get(prestacionId);
        if (duracion == null) {
            throw new IllegalArgumentException("Prestación inexistente: " + prestacionId);
        }
        return duracion;
    }
}
