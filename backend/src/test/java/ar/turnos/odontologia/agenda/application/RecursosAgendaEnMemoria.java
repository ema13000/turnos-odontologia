package ar.turnos.odontologia.agenda.application;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Doble de prueba de {@link RecursosAgenda}. Pacientes, profesionales y sillones existen y están activos
 * salvo que se marquen; las prestaciones hay que agregarlas con su duración.
 */
class RecursosAgendaEnMemoria implements RecursosAgenda {

    private final Map<Long, Prestacion> prestaciones = new HashMap<>();
    private final Set<Long> profesionalesInactivos = new HashSet<>();
    private final Set<Long> sillonesInactivos = new HashSet<>();

    void agregarPrestacion(Long prestacionId, Duration duracion) {
        prestaciones.put(prestacionId, new Prestacion(duracion, true));
    }

    void desactivarPrestacion(Long prestacionId) {
        prestaciones.computeIfPresent(prestacionId, (id, p) -> new Prestacion(p.duracion(), false));
    }

    void desactivarProfesional(Long profesionalId) {
        profesionalesInactivos.add(profesionalId);
    }

    void desactivarSillon(Long sillonId) {
        sillonesInactivos.add(sillonId);
    }

    @Override
    public boolean existePaciente(Long pacienteId) {
        return true;
    }

    @Override
    public Optional<Boolean> profesionalActivo(Long profesionalId) {
        return Optional.of(!profesionalesInactivos.contains(profesionalId));
    }

    @Override
    public Optional<Boolean> sillonActivo(Long sillonId) {
        return Optional.of(!sillonesInactivos.contains(sillonId));
    }

    @Override
    public Optional<Prestacion> prestacion(Long prestacionId) {
        return Optional.ofNullable(prestaciones.get(prestacionId));
    }
}
