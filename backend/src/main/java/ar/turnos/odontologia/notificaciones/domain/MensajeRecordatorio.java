package ar.turnos.odontologia.notificaciones.domain;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Texto de un recordatorio o de un aviso. Solo lleva la fecha, la hora, el consultorio (nombre y dirección) y, en el
 * recordatorio, el link para confirmar (RN-NO-01, regla dura 6): ni el nombre del paciente ni el del profesional. La
 * fecha y la hora van en la zona de Buenos Aires.
 */
public final class MensajeRecordatorio {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    static final String PLANTILLA_RECORDATORIO =
            "Te recordamos tu turno del %s a las %s en %s (%s). Confirmá tu asistencia acá: %s";
    static final String PLANTILLA_REASIGNACION =
            "Tu turno fue reasignado: ahora es el %s a las %s en %s (%s).";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONA);

    private final String texto;

    private MensajeRecordatorio(String texto) {
        this.texto = texto;
    }

    public static MensajeRecordatorio recordatorio(DatosDelTurno turno, Consultorio consultorio, String linkConfirmar) {
        Objects.requireNonNull(linkConfirmar, "linkConfirmar");
        return new MensajeRecordatorio(PLANTILLA_RECORDATORIO.formatted(
                FECHA.format(turno.inicio()), HORA.format(turno.inicio()),
                consultorio.nombre(), consultorio.direccion(), linkConfirmar));
    }

    /** Aviso de que el turno cambió de fecha, hora o profesional (RN-AG-16): sin link ni token. */
    public static MensajeRecordatorio reasignacion(DatosDelTurno turno, Consultorio consultorio) {
        return new MensajeRecordatorio(PLANTILLA_REASIGNACION.formatted(
                FECHA.format(turno.inicio()), HORA.format(turno.inicio()),
                consultorio.nombre(), consultorio.direccion()));
    }

    public String texto() {
        return texto;
    }
}
