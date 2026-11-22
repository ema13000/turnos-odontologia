package ar.turnos.odontologia.agenda.domain;

/** No existe un turno con el id pedido. */
public class TurnoInexistenteException extends RuntimeException {

    public TurnoInexistenteException(Long id) {
        super("No existe el turno " + id);
    }
}
