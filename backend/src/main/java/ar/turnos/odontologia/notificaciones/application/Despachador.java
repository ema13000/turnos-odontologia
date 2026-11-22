package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.application.NotificacionPort.EnvioNotificacion;
import ar.turnos.odontologia.notificaciones.application.NotificacionPort.ResultadoEnvio;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.Consultorio;
import ar.turnos.odontologia.notificaciones.domain.MensajeRecordatorio;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Arma el mensaje de una notificación, lo manda por su canal y devuelve la notificación con el resultado. Lo usan
 * el recordatorio, el reintento y el aviso de reasignación: el armado y el envío viven en un solo lugar.
 */
final class Despachador {

    private final Map<Canal, NotificacionPort> puertos = new EnumMap<>(Canal.class);
    private final Consultorio consultorio;
    private final String baseUrl;

    Despachador(List<NotificacionPort> canales, Consultorio consultorio, String baseUrl) {
        canales.forEach(p -> puertos.put(p.canal(), p));
        this.consultorio = Objects.requireNonNull(consultorio, "consultorio");
        String base = Objects.requireNonNull(baseUrl, "baseUrl");
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /**
     * @param token el token en claro del link de confirmación; {@code null} si el tipo no lleva link
     * @return la notificación {@code ENVIADA}, o {@code FALLIDA} con su próximo intento
     */
    Notificacion despachar(Notificacion notificacion, TurnoARecordar turno, String token, Instant ahora) {
        EnvioNotificacion envio = new EnvioNotificacion(notificacion.id(),
                turno.contacto().destino(notificacion.canal()), cuerpo(notificacion, turno, token));
        return enviar(notificacion.canal(), envio)
                ? notificacion.enviada(ahora)
                : notificacion.fallida(ahora, turno.contacto().tieneEmail());
    }

    private String cuerpo(Notificacion notificacion, TurnoARecordar turno, String token) {
        return switch (notificacion.tipo()) {
            case RECORDATORIO -> MensajeRecordatorio.recordatorio(turno.datosDelTurno(), consultorio,
                    baseUrl + "/confirmar/" + Objects.requireNonNull(token, "token")).texto();
            case REASIGNACION -> MensajeRecordatorio.reasignacion(turno.datosDelTurno(), consultorio).texto();
            default -> throw new IllegalArgumentException("Tipo sin mensaje todavía: " + notificacion.tipo());
        };
    }

    private boolean enviar(Canal canal, EnvioNotificacion envio) {
        NotificacionPort puerto = puertos.get(canal);
        if (puerto == null) {
            return false;
        }
        try {
            return puerto.enviar(envio) == ResultadoEnvio.OK;
        } catch (RuntimeException e) {
            // el proveedor falló de forma inesperada: se trata como una falla de envío y se reintenta
            return false;
        }
    }
}
