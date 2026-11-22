package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;

/**
 * La base rechazó el sobreturno en un sillón que el dominio vio libre: otra operación lo tomó en el medio.
 * Una violación de {@code EXCLUDE} deja abortada la transacción de PostgreSQL, así que el caso de uso no puede
 * seguir con el próximo sillón en la misma transacción: informa cuál se tomó para que quien lo invoca
 * reintente en una transacción nueva sin ese sillón. Si nadie lo reintenta, es un rechazo común
 * {@code SIN_SILLON_DISPONIBLE}.
 */
public class SillonTomadoException extends TurnoRechazadoException {

    private final Long sillonId;

    public SillonTomadoException(Long sillonId) {
        super(CodigoRechazo.SIN_SILLON_DISPONIBLE, "No hay un sillón libre con el margen de limpieza");
        this.sillonId = sillonId;
    }

    public Long sillonId() {
        return sillonId;
    }
}
