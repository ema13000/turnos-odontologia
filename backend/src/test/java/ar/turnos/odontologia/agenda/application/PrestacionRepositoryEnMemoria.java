package ar.turnos.odontologia.agenda.application;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Doble de prueba de {@link PrestacionRepository}. */
class PrestacionRepositoryEnMemoria implements PrestacionRepository {

    private final Map<Long, Duration> duraciones = new HashMap<>();

    void agregar(Long prestacionId, Duration duracion) {
        duraciones.put(prestacionId, duracion);
    }

    @Override
    public Duration duracionDe(Long prestacionId) {
        Duration duracion = duraciones.get(prestacionId);
        if (duracion == null) {
            throw new IllegalArgumentException("Prestación inexistente: " + prestacionId);
        }
        return duracion;
    }
}
