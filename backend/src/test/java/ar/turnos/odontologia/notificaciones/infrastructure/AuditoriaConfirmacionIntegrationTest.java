package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import ar.turnos.odontologia.agenda.infrastructure.ServicioAgenda;
import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Escenarios del spec {@code auditoria-turnos} de C-11: la acción CONFIRMAR, leyendo la tabla turno_auditoria. */
@Tag("integration")
class AuditoriaConfirmacionIntegrationTest extends IntegracionNotificacionesBase {

    @Autowired ServicioNotificaciones servicio;
    @Autowired ServicioAgenda agenda;

    /** Turno RESERVADO de "Paciente Uno" con su recordatorio enviado; devuelve el id del turno. */
    private long turnoConRecordatorio() {
        contacto(pacienteUno, true, true, "paciente.uno@example.com", "+54 11 5555-0001");
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        reservadoEn(turno, en(2026, 11, 28, 12, 0));
        servicio.enviarRecordatorios();
        return turno;
    }

    private List<Map<String, Object>> registrosConfirmar(long turno) {
        return jdbc.queryForList("SELECT turno_id, accion, actor, fecha, turno_relacionado_id FROM turno_auditoria"
                + " WHERE turno_id = ? AND accion = 'CONFIRMAR'", turno);
    }

    @Test
    @DisplayName("Se registra la confirmación")
    void seRegistraLaConfirmacion() {
        // GIVEN un turno RESERVADO de "Paciente Uno" con un token sin usar
        long turno = turnoConRecordatorio();
        String token = envios.ultimoToken();

        // WHEN el paciente confirma con ese token
        servicio.confirmar(token);

        // THEN existe un registro de auditoría con la acción CONFIRMAR, el id de ese turno y el actor paciente-token
        List<Map<String, Object>> registros = registrosConfirmar(turno);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0)).containsEntry("turno_id", turno).containsEntry("actor", "paciente-token")
                .containsEntry("turno_relacionado_id", null);
        assertThat(registros.get(0).get("fecha")).isNotNull();
        // AND ningún campo del registro contiene el token
        assertThat(jdbc.queryForObject("SELECT string_agg(concat_ws('|', turno_id, accion, actor, fecha,"
                + " turno_relacionado_id), ' ') FROM turno_auditoria", String.class))
                .doesNotContain(token);
    }

    @Test
    @DisplayName("Confirmación rechazada sin registro")
    void confirmacionRechazadaSinRegistro() {
        // GIVEN un turno cancelado de "Paciente Uno" con un token sin usar
        long turno = turnoConRecordatorio();
        String token = envios.ultimoToken();
        jdbc.update("UPDATE turno SET estado = 'CANCELADO' WHERE id = ?", turno);

        // WHEN se intenta confirmar con ese token y la operación se rechaza
        assertThatThrownBy(() -> servicio.confirmar(token)).isInstanceOf(ConfirmacionRechazadaException.class);

        // THEN no existe ningún registro CONFIRMAR para ese turno
        assertThat(registrosConfirmar(turno)).isEmpty();
    }

    @Test
    void laAgendaAuditaConElActorQueLaLlama() {
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");

        agenda.confirmar(turno, "paciente-token");

        assertThat(registrosConfirmar(turno)).extracting(r -> r.get("actor")).containsExactly("paciente-token");
        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turno))
                .isEqualTo("CONFIRMADO");
    }

    @Test
    void unTurnoQueNoEstaReservadoSeRechazaEnLaAgendaSinRegistro() {
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "ATENDIDO");

        assertThatThrownBy(() -> agenda.confirmar(turno, "paciente-token")).isInstanceOf(TurnoRechazadoException.class);

        assertThat(registrosConfirmar(turno)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turno))
                .isEqualTo("ATENDIDO");
    }
}
