package ar.turnos.odontologia.notificaciones.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Registro de lectura de un turno que puede recibir un mensaje: solo lo necesario para armarlo y para decidir si
 * se envía. Nunca la entidad del turno ni nombres, prestación o motivo (design D2, regla dura 6).
 *
 * @param reservadoEn cuándo se reservó el turno, o {@code null} si no hay registro (un turno anterior a la
 *     auditoría se trata como reservado con tiempo)
 * @param activo {@code true} si el turno está {@code RESERVADO} o {@code CONFIRMADO}
 */
public record TurnoARecordar(Long turnoId, Instant inicio, Instant reservadoEn, boolean activo,
                             ContactoPaciente contacto) {

    /** Anticipación mínima de la reserva y ventana del recordatorio (RN-NO-04). */
    public static final Duration ANTICIPACION = Duration.ofHours(24);

    public TurnoARecordar {
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(contacto, "contacto");
    }

    public DatosDelTurno datosDelTurno() {
        return new DatosDelTurno(inicio);
    }

    /** El turno empieza después de {@code ahora} y como mucho 24 horas después (el límite exacto entra). */
    public boolean empiezaDentroDeLasProximas24Horas(Instant ahora) {
        return inicio.isAfter(ahora) && !inicio.isAfter(ahora.plus(ANTICIPACION));
    }

    /** Se reservó con 24 horas o más de anticipación respecto de su inicio; sin registro, se asume que sí. */
    public boolean reservadoConAnticipacion() {
        return reservadoEn == null || Duration.between(reservadoEn, inicio).compareTo(ANTICIPACION) >= 0;
    }
}
