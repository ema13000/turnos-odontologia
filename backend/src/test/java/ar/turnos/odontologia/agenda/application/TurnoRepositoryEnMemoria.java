package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.util.ArrayList;
import java.util.List;

/** Doble de prueba de {@link TurnoRepository}. Asigna ids correlativos, como una identidad BIGINT. */
class TurnoRepositoryEnMemoria implements TurnoRepository {

    private final List<Turno> turnos = new ArrayList<>();
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
        Turno guardado = turno.id() == null ? turno.conId(proximoId++) : turno;
        turnos.add(guardado);
        return guardado;
    }

    List<Turno> todos() {
        return List.copyOf(turnos);
    }
}
