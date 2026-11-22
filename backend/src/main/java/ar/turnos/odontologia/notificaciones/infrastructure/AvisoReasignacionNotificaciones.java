package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.agenda.disponibilidad.application.AvisoReasignacion;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link AvisoReasignacion} de la agenda (C-07, design D10): pide el aviso de reasignación
 * de C-11 para el turno nuevo. Las reglas de consentimiento, canal, contenido y reintentos son de
 * {@link ServicioNotificaciones#avisarReasignacion}. La agenda no conoce este módulo: depende solo del puerto.
 */
@Component
class AvisoReasignacionNotificaciones implements AvisoReasignacion {

    private final ServicioNotificaciones notificaciones;

    AvisoReasignacionNotificaciones(ServicioNotificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    @Override
    public void avisar(Long turnoId) {
        notificaciones.avisarReasignacion(turnoId);
    }
}
