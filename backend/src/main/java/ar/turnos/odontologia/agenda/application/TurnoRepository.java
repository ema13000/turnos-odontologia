package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de turnos. */
public interface TurnoRepository {

    /**
     * Turnos del profesional o del sillón cuyo intervalo se cruza con la ventana, en cualquier
     * estado. El filtro de turnos activos lo aplica el dominio.
     */
    List<Turno> buscarQueIntersecan(Long profesionalId, Long sillonId, Intervalo ventana);

    /**
     * Guarda el turno: lo inserta si no tiene id y, si lo tiene, actualiza su estado. Devuelve el turno
     * con el identificador que le asignó la base.
     *
     * @throws ar.turnos.odontologia.agenda.domain.TurnoRechazadoException si la base detecta un
     *     solapamiento que el dominio no vio (reservas simultáneas, RN-AG-12)
     */
    Turno guardar(Turno turno);

    /** Busca un turno para cancelarlo o reprogramarlo, bloqueándolo hasta el fin de la operación. */
    Optional<TurnoModificable> buscarParaModificar(Long turnoId);

    /** Un turno junto con el DNI de su paciente, para validar la identidad (RN-AG-07). */
    record TurnoModificable(Turno turno, String dniPaciente) {}
}
