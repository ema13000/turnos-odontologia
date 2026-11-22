package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import ar.turnos.odontologia.notificaciones.application.TurnosARecordar;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Consulta de turnos a recordar contra PostgreSQL real (design D2). "Ahora" es el 2026-12-01 a las 08:00; la
 * ventana llega hasta el 2026-12-02 a las 08:00.
 */
@Tag("integration")
class TurnosARecordarJpaIntegrationTest extends IntegracionNotificacionesBase {

    private static final Instant AHORA = en(2026, 12, 1, 8, 0);

    @Autowired TurnosARecordar turnos;

    private long consentido(long paciente) {
        contacto(paciente, true, true, "paciente@example.com", "+54 11 5555-0001");
        return paciente;
    }

    private List<Long> candidatos() {
        return turnos.candidatos(AHORA).stream().map(TurnoARecordar::turnoId).toList();
    }

    @Test
    void traeLosTurnosActivosDeLasProximas24HorasConConsentimiento() {
        consentido(pacienteUno);
        long reservado = turno(pacienteUno, draA, sillon1, en(2026, 12, 1, 9, 0), "RESERVADO");
        long confirmado = turno(pacienteUno, draA, sillon1, en(2026, 12, 1, 10, 0), "CONFIRMADO");
        turno(pacienteUno, draA, sillon1, en(2026, 12, 1, 11, 0), "CANCELADO");
        turno(pacienteUno, draA, sillon1, en(2026, 12, 1, 12, 0), "ATENDIDO");
        turno(pacienteUno, draA, sillon1, en(2026, 12, 1, 13, 0), "AUSENTE");

        assertThat(candidatos()).containsExactlyInAnyOrder(reservado, confirmado);
    }

    @Test
    void excluyeLosTurnosFueraDeLaVentana() {
        consentido(pacienteUno);
        turno(pacienteUno, draA, sillon1, en(2026, 12, 1, 7, 30), "RESERVADO");        // ya empezó
        long limite = turno(pacienteUno, draA, sillon1, en(2026, 12, 2, 8, 0), "RESERVADO");   // justo 24 h
        turno(pacienteUno, draA, sillon1, en(2026, 12, 2, 9, 0), "RESERVADO");         // 25 h

        assertThat(candidatos()).containsExactly(limite);
    }

    @Test
    void excluyeALosPacientesSinConsentimiento() {
        contacto(pacienteUno, false, true, "paciente.uno@example.com", "+54 11 5555-0001");
        consentido(pacienteDos);
        turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        long conConsentimiento = turno(pacienteDos, drB, sillon2, turnoDeManana(), "RESERVADO");

        assertThat(candidatos()).containsExactly(conConsentimiento);
    }

    @Test
    void excluyeLosTurnosQueYaTienenRecordatorio() {
        consentido(pacienteUno);
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        jdbc.update("INSERT INTO notificacion(turno_id, canal, tipo, estado, programada_para)"
                + " VALUES (?, 'WHATSAPP', 'RECORDATORIO', 'ENVIADA', ?)", turno, Timestamp.from(AHORA));

        assertThat(candidatos()).isEmpty();
    }

    @Test
    void unaReasignacionPreviaNoImpideElRecordatorio() {
        consentido(pacienteUno);
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        jdbc.update("INSERT INTO notificacion(turno_id, canal, tipo, estado, programada_para)"
                + " VALUES (?, 'EMAIL', 'REASIGNACION', 'ENVIADA', ?)", turno, Timestamp.from(AHORA));

        assertThat(candidatos()).containsExactly(turno);
    }

    @Test
    void aplicaLaAnticipacionDeLaReservaSegunLaAuditoria() {
        consentido(pacienteUno);
        consentido(pacienteDos);
        // reservado el mismo día: 22 h 30 min de anticipación
        long mismoDia = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        reservadoEn(mismoDia, en(2026, 12, 1, 9, 0));
        // reservado con tiempo
        long conTiempo = turno(pacienteDos, drB, sillon2, turnoDeManana(), "RESERVADO");
        reservadoEn(conTiempo, en(2026, 11, 28, 12, 0));

        assertThat(candidatos()).containsExactly(conTiempo);
    }

    @Test
    void elTurnoSinRegistroDeAuditoriaSeTrataComoReservadoConTiempo() {
        consentido(pacienteUno);
        long sinAuditoria = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");

        assertThat(candidatos()).containsExactly(sinAuditoria);
        assertThat(turnos.candidatos(AHORA).get(0).reservadoEn()).isNull();
    }

    @Test
    void laAnticipacionExactaDe24HorasEntraYLaPrimeraAuditoriaEsLaQueCuenta() {
        consentido(pacienteUno);
        long turno = turno(pacienteUno, draA, sillon1, en(2026, 12, 2, 7, 0), "RESERVADO");
        reservadoEn(turno, en(2026, 12, 1, 7, 0));          // exactamente 24 horas antes
        reservadoEn(turno, en(2026, 12, 1, 7, 50));         // un registro posterior (reprogramación) no cuenta

        assertThat(candidatos()).containsExactly(turno);
    }

    @Test
    void armaElRegistroDeLecturaSoloConLoNecesario() {
        contacto(pacienteUno, true, true, "paciente.uno@example.com", "+54 11 5555-0001");
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        reservadoEn(turno, en(2026, 11, 28, 12, 0));

        TurnoARecordar leido = turnos.candidatos(AHORA).get(0);

        assertThat(leido.inicio()).isEqualTo(turnoDeManana());
        assertThat(leido.reservadoEn()).isEqualTo(en(2026, 11, 28, 12, 0));
        assertThat(leido.activo()).isTrue();
        assertThat(leido.contacto().canalDeEnvio()).contains(Canal.WHATSAPP);
        assertThat(leido.contacto().destino(Canal.EMAIL)).isEqualTo("paciente.uno@example.com");
    }

    @Test
    void devuelveUnTurnoPorIdEnCualquierEstadoYVacioSiNoExiste() {
        consentido(pacienteUno);
        long cancelado = turno(pacienteUno, draA, sillon1, turnoDeManana(), "CANCELADO");

        assertThat(turnos.deTurno(cancelado)).hasValueSatisfying(t -> assertThat(t.activo()).isFalse());
        assertThat(turnos.deTurno(999_999L)).isEmpty();
    }
}
