package ar.turnos.odontologia.notificaciones.application;

/**
 * Puerto de salida hacia la agenda (design D3): pasa un turno {@code RESERVADO} a {@code CONFIRMADO}. El puerto no
 * recibe el token: la auditoría de la agenda nunca lo ve.
 */
public interface ConfirmadorDeTurnos {

    /**
     * @throws ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException con
     *     {@code TURNO_NO_CONFIRMABLE} si el turno ya no está {@code RESERVADO} o no existe
     */
    TurnoConfirmado confirmar(Long turnoId);
}
