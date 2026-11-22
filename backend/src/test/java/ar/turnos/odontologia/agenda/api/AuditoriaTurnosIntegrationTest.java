package ar.turnos.odontologia.agenda.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Un test por escenario del spec {@code auditoria-turnos} (C-06), leyendo la tabla turno_auditoria. */
@Tag("integration")
class AuditoriaTurnosIntegrationTest extends IntegracionAgendaBase {

    // ---------- Requirement: Registro de cada operación sobre un turno ----------

    @Test
    @DisplayName("Se registra la creación")
    void seRegistraLaCreacion() throws Exception {
        // GIVEN recepcion con sesión
        // WHEN crea un turno de "Paciente Uno" a las 10:00
        JsonNode turno = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        // THEN existe un registro de auditoría con la acción CREAR, el id de ese turno y el actor recepcion
        assertThat(registros(turno.get("id").asLong()))
                .containsExactly(Map.of("accion", "CREAR", "actor", "recepcion"));
        assertThat(jdbc.queryForObject("SELECT fecha IS NOT NULL FROM turno_auditoria WHERE turno_id = ?",
                Boolean.class, turno.get("id").asLong())).isTrue();
    }

    @Test
    @DisplayName("Se registra la cancelación")
    void seRegistraLaCancelacion() throws Exception {
        // GIVEN recepcion con sesión AND un turno activo de "Paciente Uno" a las 10:00
        JsonNode turno = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        // WHEN lo cancela con el DNI y el número de reserva correctos
        MvcResult resultado = cancelar(como("recepcion"), turno.get("id").asLong(), DNI_PACIENTE_UNO,
                turno.get("numeroReserva").asText());
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

        // THEN existe un registro de auditoría con la acción CANCELAR, el id de ese turno y el actor recepcion
        assertThat(registros(turno.get("id").asLong()))
                .contains(Map.of("accion", "CANCELAR", "actor", "recepcion"));
    }

    // ---------- Requirement: La reprogramación vincula los dos turnos ----------

    @Test
    @DisplayName("Se registra la reprogramación")
    void seRegistraLaReprogramacion() throws Exception {
        // GIVEN recepcion con sesión AND un turno activo de "Paciente Uno" a las 10:00
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        // WHEN lo reprograma a las 11:00
        JsonNode nuevo = cuerpo(reprogramar(como("recepcion"), original.get("id").asLong(),
                original.get("numeroReserva").asText(), hora(11, 0), null, null));

        // THEN existe un registro REPROGRAMAR sobre el turno original que indica el id del turno nuevo
        assertThat(jdbc.queryForObject(
                "SELECT turno_relacionado_id FROM turno_auditoria WHERE turno_id = ? AND accion = 'REPROGRAMAR'",
                Long.class, original.get("id").asLong())).isEqualTo(nuevo.get("id").asLong());
        // AND existe un registro CREAR sobre el turno nuevo
        assertThat(registros(nuevo.get("id").asLong()))
                .containsExactly(Map.of("accion", "CREAR", "actor", "recepcion"));
    }

    // ---------- Requirement: Solo se auditan las operaciones que ocurrieron ----------

    @Test
    @DisplayName("Reprogramación rechazada sin registro")
    void reprogramacionRechazadaSinRegistro() throws Exception {
        // GIVEN recepcion con sesión AND un turno activo de "Paciente Uno" a las 10:00 y otro a las 11:00 en el Sillón 1
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));
        turnoCreado(pacienteDos, drB, sillon1, hora(11, 0));

        // WHEN reprograma el primero a las 11:10 en el Sillón 1 y la operación se rechaza
        MvcResult resultado = reprogramar(como("recepcion"), original.get("id").asLong(),
                original.get("numeroReserva").asText(), hora(11, 10), null, sillon1);
        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);

        // THEN no existe ningún registro REPROGRAMAR para el turno original
        assertThat(registros(original.get("id").asLong()))
                .containsExactly(Map.of("accion", "CREAR", "actor", "recepcion"));
    }

    // ---------- Requirement: Sin datos sensibles en la auditoría ----------

    @Test
    @DisplayName("La cancelación no guarda el DNI ni el número de reserva")
    void laCancelacionNoGuardaDatosSensibles() throws Exception {
        // GIVEN recepcion con sesión AND un turno activo de "Paciente Uno" a las 10:00
        JsonNode turno = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));
        String numeroReserva = turno.get("numeroReserva").asText();

        // WHEN lo cancela con el DNI 99000001 y el número de reserva del turno
        cancelar(como("recepcion"), turno.get("id").asLong(), DNI_PACIENTE_UNO, numeroReserva);

        // THEN el registro CANCELAR no contiene 99000001 ni el número de reserva
        String registro = jdbc.queryForObject(
                "SELECT row_to_json(a)::text FROM turno_auditoria a WHERE turno_id = ? AND accion = 'CANCELAR'",
                String.class, turno.get("id").asLong());
        assertThat(registro).isNotBlank().doesNotContain(DNI_PACIENTE_UNO, numeroReserva);
    }

    private List<Map<String, Object>> registros(long turnoId) {
        return jdbc.queryForList("SELECT accion, actor FROM turno_auditoria WHERE turno_id = ? ORDER BY id", turnoId);
    }
}
