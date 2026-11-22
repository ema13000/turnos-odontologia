package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.notificaciones.application.NotificacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Canal simulado (DD-04): no envía nada de verdad. Solo registra una línea con el canal, el id de la notificación
 * y el resultado. Nunca escribe el cuerpo, el token, el teléfono ni el email, ni siquiera si el envío falla
 * (RN-NO-01, regla dura 6). Se puede configurar para fallar, así los reintentos tienen un camino real de punta a
 * punta sin un proveedor.
 */
abstract class CanalSimulado implements NotificacionPort {

    private static final Logger LOG = LoggerFactory.getLogger(CanalSimulado.class);

    private final boolean falla;

    CanalSimulado(boolean falla) {
        this.falla = falla;
    }

    @Override
    public ResultadoEnvio enviar(EnvioNotificacion envio) {
        ResultadoEnvio resultado = falla ? ResultadoEnvio.FALLO : ResultadoEnvio.OK;
        LOG.info("Envío simulado: canal={} notificacion={} resultado={}", canal(), envio.notificacionId(), resultado);
        return resultado;
    }
}
