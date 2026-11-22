package ar.turnos.odontologia.agenda.disponibilidad.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aviso que no envía nada: se usa mientras ningún módulo publique un {@link AvisoReasignacion} (design D10). Deja
 * una línea de log con el id del turno, sin datos del paciente.
 */
final class AvisoReasignacionInactivo implements AvisoReasignacion {

    private static final Logger LOG = LoggerFactory.getLogger(AvisoReasignacionInactivo.class);

    @Override
    public void avisar(Long turnoId) {
        LOG.info("Aviso de reasignación del turno {} sin enviar: no hay un módulo de notificaciones", turnoId);
    }
}
