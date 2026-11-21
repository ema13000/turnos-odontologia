package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Doble de prueba de {@link TurnoRepository}. */
class TurnoRepositoryEnMemoria implements TurnoRepository {

    private final List<Turno> turnos = new ArrayList<>();

    @Override
    public List<Turno> buscarQueIntersecan(UUID profesionalId, UUID sillonId, Intervalo ventana) {
        return turnos.stream()
                .filter(t -> t.profesionalId().equals(profesionalId) || t.sillonId().equals(sillonId))
                .filter(t -> t.intervalo().seSuperponeCon(ventana))
                .toList();
    }

    @Override
    public void guardar(Turno turno) {
        turnos.add(turno);
    }

    List<Turno> todos() {
        return List.copyOf(turnos);
    }
}
