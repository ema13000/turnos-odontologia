package ar.turnos.odontologia.agenda.disponibilidad.application;

/** El profesional, la ausencia o el bloqueo no existe: {@code 404 RECURSO_NO_ENCONTRADO}, como en C-04. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso) {
        super("No existe " + recurso);
    }
}
