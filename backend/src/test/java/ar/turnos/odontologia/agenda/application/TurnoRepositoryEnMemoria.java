package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba de {@link TurnoRepository}. Asigna ids correlativos, como una identidad BIGINT. */
class TurnoRepositoryEnMemoria implements TurnoRepository {

    private final List<Turno> turnos = new ArrayList<>();
    private final Map<Long, String> dniPorPaciente = new HashMap<>();
    private long proximoId = 1;

    @Override
    public List<Turno> buscarQueIntersecan(Long profesionalId, Long sillonId, Intervalo ventana) {
        return turnos.stream()
                .filter(t -> t.profesionalId().equals(profesionalId) || t.sillonId().equals(sillonId))
                .filter(t -> t.intervalo().seSuperponeCon(ventana))
                .toList();
    }

    @Override
    public Turno guardar(Turno turno) {
        if (turno.id() == null) {
            Turno guardado = turno.conId(proximoId++);
            turnos.add(guardado);
            return guardado;
        }
        turnos.replaceAll(t -> t.id().equals(turno.id()) ? turno : t);
        return turno;
    }

    @Override
    public Optional<TurnoModificable> buscarParaModificar(Long turnoId) {
        return turnos.stream()
                .filter(t -> t.id().equals(turnoId))
                .findFirst()
                .map(t -> new TurnoModificable(t, dniPorPaciente.get(t.pacienteId())));
    }

    void registrarDni(Long pacienteId, String dni) {
        dniPorPaciente.put(pacienteId, dni);
    }

    Turno porId(Long id) {
        return turnos.stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
    }

    List<Turno> todos() {
        return List.copyOf(turnos);
    }
}
