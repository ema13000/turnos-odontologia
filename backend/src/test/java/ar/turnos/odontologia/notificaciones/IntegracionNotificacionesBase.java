package ar.turnos.odontologia.notificaciones;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Base de los tests de integración de notificaciones (C-11): hereda la base de la agenda (PostgreSQL real, reloj
 * fijo 2026-12-01 08:00 en Buenos Aires, datos ficticios) y apaga la tarea programada: los tests llaman a los
 * casos de uso a mano, así no dependen del tiempo real.
 */
@TestPropertySource(properties = {
        "app.notificaciones.activo=false",
        "app.notificaciones.base-url=https://turnos.example.test",
        "app.consultorio.nombre=Clínica Ejemplo",
        "app.consultorio.direccion=Calle Ficticia 123"})
@Import(CapturaDeEnvios.class)
public abstract class IntegracionNotificacionesBase extends IntegracionAgendaBase {

    @Autowired protected CapturaDeEnvios.Registro envios;

    @BeforeEach
    void reiniciarCapturaDeEnvios() {
        envios.reiniciar();
    }

    /** Instante local de Buenos Aires. */
    protected static Instant en(int anio, int mes, int dia, int hora, int minuto) {
        return LocalDateTime.of(anio, mes, dia, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }

    /** El turno de los escenarios: 2026-12-02 a las 07:30 (dentro de las próximas 24 horas del 2026-12-01 08:00). */
    protected static Instant turnoDeManana() {
        return en(2026, 12, 2, 7, 30);
    }

    /** Datos de contacto de un paciente ficticio ya creado. */
    protected void contacto(long paciente, boolean consentimiento, boolean whatsapp, String email, String telefono) {
        jdbc.update("UPDATE paciente SET consentimiento_comunicaciones = ?, tiene_whatsapp = ?, email = ?, telefono = ?,"
                        + " consentimiento_fecha = CASE WHEN ? THEN now() ELSE NULL END WHERE id = ?",
                consentimiento, whatsapp, email, telefono, consentimiento, paciente);
    }

    /** Inserta un turno de 30 minutos (margen de 10) y devuelve su id. */
    protected long turno(long paciente, long profesional, long sillon, Instant inicio, String estado) {
        Instant fin = inicio.plusSeconds(30 * 60);
        return jdbc.queryForObject("INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, inicio,"
                        + " fin, fin_con_margen, estado) VALUES (?,?,?,?,?,?,?,?) RETURNING id", Long.class,
                profesional, sillon, consulta, paciente, Timestamp.from(inicio), Timestamp.from(fin),
                Timestamp.from(fin.plusSeconds(10 * 60)), estado);
    }

    /** Registra en la auditoría cuándo se reservó el turno (lo único que lo guarda, design D2). */
    protected void reservadoEn(long turnoId, Instant momento) {
        jdbc.update("INSERT INTO turno_auditoria(turno_id, accion, actor, fecha) VALUES (?, 'CREAR', 'recepcion', ?)",
                turnoId, Timestamp.from(momento));
    }
}
