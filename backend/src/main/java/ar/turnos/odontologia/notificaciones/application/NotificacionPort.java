package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.domain.Canal;

/**
 * Puerto de salida: un canal de envío (WhatsApp o email). En el TP los dos adaptadores son simulados (DD-04).
 * Un adaptador nunca escribe en un log el cuerpo, el token ni el contacto completo (RN-NO-01, regla dura 6).
 */
public interface NotificacionPort {

    Canal canal();

    ResultadoEnvio enviar(EnvioNotificacion envio);

    enum ResultadoEnvio { OK, FALLO }

    /** Lo que se manda: id de la notificación, a dónde y qué texto. */
    record EnvioNotificacion(Long notificacionId, String destino, String cuerpo) {

        @Override
        public String toString() {
            // nunca el destino ni el cuerpo: pueden terminar en un log
            return "EnvioNotificacion[notificacionId=" + notificacionId + "]";
        }
    }
}
