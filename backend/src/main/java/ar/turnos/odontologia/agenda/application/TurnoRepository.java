package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: almacenamiento de turnos. La implementación con PostgreSQL llega con C-03 y C-06. */
public interface TurnoRepository {

    /**
     * Turnos del profesional o del sillón cuyo intervalo se cruza con la ventana, en cualquier
     * estado. El filtro de turnos activos lo aplica el dominio.
     */
    List<Turno> buscarQueIntersecan(UUID profesionalId, UUID sillonId, Intervalo ventana);

    void guardar(Turno turno);
}
