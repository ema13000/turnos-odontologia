package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.Consultorio;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TipoNotificacion;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: avisa a un paciente que su turno fue reasignado por la ausencia del profesional (RN-AG-16), con
 * las mismas reglas de consentimiento, canal y contenido que el recordatorio. El aviso no lleva token ni link. C-07
 * lo llama al reasignar, después de guardar el turno con su nueva fecha, hora y profesional.
 *
 * <p>Cada reasignación genera su aviso: no hay límite de uno por turno. Si el envío falla, el aviso queda
 * {@code FALLIDA} y {@link EnviarRecordatorios} lo reintenta.
 */
public class AvisarReasignacion {

    private final TurnosARecordar turnos;
    private final NotificacionRepository notificaciones;
    private final Despachador despachador;
    private final Clock reloj;

    public AvisarReasignacion(TurnosARecordar turnos, NotificacionRepository notificaciones,
                              List<NotificacionPort> canales, Consultorio consultorio, Clock reloj) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
        this.notificaciones = Objects.requireNonNull(notificaciones, "notificaciones");
        this.despachador = new Despachador(canales, consultorio, "");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /** @return el aviso, o vacío si el turno no existe o el paciente no puede recibir comunicaciones */
    public Optional<Notificacion> ejecutar(Long turnoId) {
        Optional<TurnoARecordar> encontrado = turnos.deTurno(turnoId);
        if (encontrado.isEmpty() || !encontrado.get().contacto().puedeRecibirComunicaciones()) {
            return Optional.empty();
        }
        TurnoARecordar turno = encontrado.get();
        Canal canal = turno.contacto().canalDeEnvio().orElseThrow();
        Instant ahora = reloj.instant();
        Notificacion aviso = notificaciones.insertar(
                Notificacion.pendiente(turno.turnoId(), canal, TipoNotificacion.REASIGNACION, ahora, null));
        Notificacion resultado = despachador.despachar(aviso, turno, null, ahora);
        notificaciones.actualizar(resultado);
        return Optional.of(resultado);
    }
}
