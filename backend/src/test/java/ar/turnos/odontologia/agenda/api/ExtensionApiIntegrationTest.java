package ar.turnos.odontologia.agenda.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.shared.security.Navegador;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Un test por escenario del delta {@code agenda-api} correspondiente a las extensiones (C-08). */
@Tag("integration")
class ExtensionApiIntegrationTest extends IntegracionAgendaBase {

    private static final String MOTIVO = "Complicación administrativa ficticia";

    @Test
    @DisplayName("Recepción extiende un turno")
    void recepcionExtiendeUnTurno() throws Exception {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        // WHEN pide POST /api/v1/turnos/{id}/extender con fin 10:45 y un motivo administrativo
        MvcResult resultado = extender(como("recepcion"), original.get("id").asLong(), hora(10, 45), MOTIVO);

        // THEN la respuesta es 200 con fin 10:45, extendido verdadero y el mismo numeroReserva
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode extendido = cuerpo(resultado);
        assertThat(extendido.get("fin").asText()).isEqualTo(hora(10, 45));
        assertThat(extendido.get("extendido").asBoolean()).isTrue();
        assertThat(extendido.get("numeroReserva").asText()).isEqualTo(original.get("numeroReserva").asText());
        assertThat(extendido.get("id").asLong()).isEqualTo(original.get("id").asLong());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Extensión con fin sin zona horaria")
    void extensionConFinSinZonaHoraria() throws Exception {
        // GIVEN recepcion con sesión iniciada AND un turno activo
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("fin", "2026-12-01T10:45:00");
        pedido.put("motivo", MOTIVO);

        // WHEN pide extender un turno con fin 2026-12-01T10:45:00
        MvcResult resultado = como("recepcion").post(TURNOS + "/" + original.get("id").asLong() + "/extender",
                json.writeValueAsString(pedido)).andReturn();

        // THEN la respuesta es 422 con el código DATOS_INVALIDOS y el campo fin
        assertProblema(resultado, 422, "DATOS_INVALIDOS");
        assertThat(camposConError(resultado)).contains("fin");
    }

    @Test
    @DisplayName("Extensión de un turno inexistente")
    void extensionDeUnTurnoInexistente() throws Exception {
        // GIVEN recepcion con sesión iniciada
        // WHEN pide POST /api/v1/turnos/999999/extender con un fin y un motivo válidos
        MvcResult resultado = extender(como("recepcion"), 999999L, hora(10, 45), MOTIVO);

        // THEN la respuesta es 404 con el código TURNO_INEXISTENTE
        assertProblema(resultado, 404, "TURNO_INEXISTENTE");
    }

    @Test
    @DisplayName("Extensión rechazada no reprograma el siguiente")
    void extensionRechazadaNoReprogramaElSiguiente() throws Exception {
        // GIVEN un turno activo de 10:00 a 10:30 y otro de 11:00 a 11:30 en el mismo sillón
        JsonNode primero = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));
        JsonNode siguiente = turnoCreado(pacienteDos, drB, sillon1, hora(11, 0));

        // WHEN pide extender el primero hasta las 11:00 y la operación se rechaza
        MvcResult resultado = extender(como("recepcion"), primero.get("id").asLong(), hora(11, 0), MOTIVO);

        // THEN la respuesta es 409 con el código SOLAPAMIENTO_SILLON
        assertProblema(resultado, 409, "SOLAPAMIENTO_SILLON");
        // AND el turno siguiente sigue en su horario original
        assertThat(jdbc.queryForObject("SELECT inicio = ?::timestamptz AND fin = ?::timestamptz "
                        + "FROM turno WHERE id = ?", Boolean.class,
                hora(11, 0), hora(11, 30), siguiente.get("id").asLong())).isTrue();
        assertThat(jdbc.queryForObject("SELECT fin = ?::timestamptz FROM turno WHERE id = ?", Boolean.class,
                hora(10, 30), primero.get("id").asLong())).isTrue();
    }

    @Test
    @DisplayName("Una extensión de un turno ya iniciado responde 200 por la API")
    void extensionDeUnTurnoYaIniciadoPorLaApi() throws Exception {
        // GIVEN un turno activo de 07:30 a 08:00 sembrado por SQL, con el reloj fijo en las 08:00
        Instant inicio = LocalDateTime.of(2026, 12, 1, 7, 30).atZone(BUENOS_AIRES).toInstant();
        Instant fin = inicio.plusSeconds(30 * 60);
        Long id = jdbc.queryForObject("INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, "
                        + "inicio, fin, fin_con_margen, estado, es_sobreturno) VALUES (?,?,?,?,?,?,?,'RESERVADO',false) "
                        + "RETURNING id", Long.class,
                draA, sillon1, consulta, pacienteUno, inicio.atOffset(ZoneOffset.UTC),
                fin.atOffset(ZoneOffset.UTC), fin.plusSeconds(10 * 60).atOffset(ZoneOffset.UTC));

        // WHEN pide extenderlo hasta las 08:30
        MvcResult resultado = extender(como("recepcion"), id, hora(8, 30), MOTIVO);

        // THEN la respuesta es 200 con fin 08:30 y extendido verdadero
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(resultado).get("fin").asText()).isEqualTo(hora(8, 30));
        assertThat(cuerpo(resultado).get("extendido").asBoolean()).isTrue();
        // AND crear un turno nuevo que empiece a las 07:30 sigue rechazado por estar en el pasado
        assertProblema(crearTurno(como("recepcion"), pacienteDos, drB, sillon2, hora(7, 30)),
                409, "INICIO_EN_EL_PASADO");
    }

    @Test
    @DisplayName("Extensión de un sobreturno")
    void extensionDeUnSobreturno() throws Exception {
        // GIVEN un sobreturno activo de 10:00 a 10:30
        MvcResult creacion = como("recepcion").post(TURNOS + "/sobreturno", json.writeValueAsString(Map.of(
                "pacienteId", pacienteUno,
                "profesionalId", draA,
                "prestacionId", consulta,
                "inicio", hora(10, 0),
                "motivo", "Urgencia administrativa ficticia"))).andReturn();
        JsonNode sobreturno = cuerpo(creacion);

        // WHEN el usuario recepcion pide extenderlo hasta las 10:45
        MvcResult resultado = extender(como("recepcion"), sobreturno.get("id").asLong(), hora(10, 45), MOTIVO);

        // THEN la respuesta es 409 con el código TURNO_NO_EXTENSIBLE
        assertProblema(resultado, 409, "TURNO_NO_EXTENSIBLE");
    }

    @Test
    @DisplayName("Un paciente intenta extender un turno")
    void unPacienteIntentaExtenderUnTurno() throws Exception {
        // GIVEN "Paciente Uno" con sesión iniciada AND un turno activo
        JsonNode turno = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        // WHEN pide POST /api/v1/turnos/{id}/extender con datos válidos
        MvcResult resultado = extender(como("paciente.uno@example.com"), turno.get("id").asLong(),
                hora(10, 45), MOTIVO);

        // THEN la respuesta es 403 con el código ACCESO_DENEGADO
        assertProblema(resultado, 403, "ACCESO_DENEGADO");
        // AND el turno queda sin cambios
        assertThat(jdbc.queryForObject("SELECT fin = ?::timestamptz AND extendido = false FROM turno WHERE id = ?",
                Boolean.class, hora(10, 30), turno.get("id").asLong())).isTrue();
    }

    @Test
    @DisplayName("Un turno normal mantiene su código de sillón")
    void unTurnoNormalMantieneSuCodigoDeSillon() throws Exception {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        // WHEN el usuario recepcion pide POST /api/v1/turnos con el Dr. B en el Sillón 1 a las 10:15
        MvcResult resultado = crearTurno(como("recepcion"), pacienteDos, drB, sillon1, hora(10, 15));

        // THEN la respuesta es 409 con el código SOLAPAMIENTO_SILLON, no SIN_SILLON_DISPONIBLE
        assertProblema(resultado, 409, "SOLAPAMIENTO_SILLON");
    }

    // ---------- Triangulación ----------

    @Test
    @DisplayName("Un fin igual al actual se rechaza con 422")
    void finIgualAlActual() throws Exception {
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        MvcResult resultado = extender(como("recepcion"), original.get("id").asLong(), hora(10, 30), MOTIVO);

        assertProblema(resultado, 422, "DATOS_INVALIDOS");
        assertThat(camposConError(resultado)).contains("fin");
    }

    @Test
    @DisplayName("Un motivo vacío en la extensión se rechaza con 422")
    void motivoVacioEnLaExtension() throws Exception {
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        MvcResult resultado = extender(como("recepcion"), original.get("id").asLong(), hora(10, 45), "   ");

        assertProblema(resultado, 422, "DATOS_INVALIDOS");
        assertThat(camposConError(resultado)).contains("motivo");
    }

    @Test
    @DisplayName("Un profesional inactivo rechaza la extensión con 409")
    void profesionalInactivo() throws Exception {
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));
        jdbc.update("UPDATE profesional SET activo = false WHERE id = ?", draA);

        MvcResult resultado = extender(como("recepcion"), original.get("id").asLong(), hora(10, 45), MOTIVO);

        assertProblema(resultado, 409, "RECURSO_INACTIVO");
        assertThat(jdbc.queryForObject("SELECT fin = ?::timestamptz FROM turno WHERE id = ?", Boolean.class,
                hora(10, 30), original.get("id").asLong())).isTrue();
    }

    // ---------- Ayudas ----------

    private MvcResult extender(Navegador navegador, long turnoId, String fin, String motivo) throws Exception {
        return navegador.post(TURNOS + "/" + turnoId + "/extender",
                json.writeValueAsString(Map.of("fin", fin, "motivo", motivo))).andReturn();
    }

    private void assertProblema(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus())
                .as(resultado.getResponse().getContentAsString())
                .isEqualTo(estado);
        assertThat(cuerpo(resultado).get("codigo").asText()).isEqualTo(codigo);
    }

    private List<String> camposConError(MvcResult resultado) throws Exception {
        List<String> campos = new ArrayList<>();
        cuerpo(resultado).get("errores").forEach(e -> campos.add(e.get("campo").asText()));
        return campos;
    }
}
