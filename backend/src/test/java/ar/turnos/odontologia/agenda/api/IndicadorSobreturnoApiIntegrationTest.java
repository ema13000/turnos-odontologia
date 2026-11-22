package ar.turnos.odontologia.agenda.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Indicador aditivo de sobreturno en las respuestas de creación y de agenda (C-08). */
@Tag("integration")
class IndicadorSobreturnoApiIntegrationTest extends IntegracionAgendaBase {

    private static final String MOTIVO = "Urgencia administrativa ficticia";

    @Test
    @DisplayName("La agenda distingue los sobreturnos")
    void laAgendaDistingueLosSobreturnos() throws Exception {
        // GIVEN un turno normal y un sobreturno activos el 2026-12-01
        JsonNode normal = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));
        JsonNode sobreturno = crearSobreturno(draA, hora(10, 0), MOTIVO);

        // WHEN el personal pide GET /api/v1/turnos para ese día
        MvcResult resultado = como("recepcion").get(agendaDelDia()).andReturn();

        // THEN ambos turnos aparecen
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode turnos = cuerpo(resultado);
        assertThat(turnos).hasSize(2);
        JsonNode enAgendaNormal = porId(turnos, normal.get("id").asLong());
        JsonNode enAgendaSobreturno = porId(turnos, sobreturno.get("id").asLong());
        // AND solo el sobreturno tiene esSobreturno verdadero
        assertThat(enAgendaNormal.get("esSobreturno").asBoolean()).isFalse();
        assertThat(enAgendaSobreturno.get("esSobreturno").asBoolean()).isTrue();
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain(MOTIVO);
    }

    @Test
    @DisplayName("Respuesta de creación sin datos sensibles")
    void respuestaDeCreacionSinDatosSensibles() throws Exception {
        // GIVEN un sobreturno creado con un motivo administrativo
        MvcResult creacion = como("recepcion").post(TURNOS + "/sobreturno", json.writeValueAsString(Map.of(
                "pacienteId", pacienteUno,
                "profesionalId", draA,
                "prestacionId", consulta,
                "inicio", hora(10, 0),
                "motivo", MOTIVO))).andReturn();

        // WHEN llega la respuesta 201 de POST /api/v1/turnos/sobreturno
        assertThat(creacion.getResponse().getStatus()).isEqualTo(201);
        String cuerpo = creacion.getResponse().getContentAsString();

        // THEN el cuerpo no incluye motivo, motivoSobreturno ni datos de salud
        assertThat(cuerpo).doesNotContain("motivo", MOTIVO, "diagnóstico", "diagnostico", "salud");
        assertThat(cuerpo(creacion).get("esSobreturno").asBoolean()).isTrue();
        assertThat(cuerpo(creacion).has("extendido")).isTrue();
    }

    // ---------- Triangulación ----------

    @Test
    @DisplayName("Una extensión se distingue con extendido en la respuesta")
    void unaExtensionSeDistingueConExtendido() throws Exception {
        JsonNode original = turnoCreado(pacienteUno, draA, sillon1, hora(10, 0));

        MvcResult resultado = como("recepcion").post(TURNOS + "/" + original.get("id").asLong() + "/extender",
                json.writeValueAsString(Map.of("fin", hora(10, 45), "motivo", "Ajuste administrativo"))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode extendido = cuerpo(resultado);
        assertThat(extendido.get("extendido").asBoolean()).isTrue();
        assertThat(extendido.get("esSobreturno").asBoolean()).isFalse();
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("Ajuste administrativo", "motivo");
    }

    private JsonNode crearSobreturno(long profesional, String inicio, String motivo) throws Exception {
        MvcResult resultado = como("recepcion").post(TURNOS + "/sobreturno", json.writeValueAsString(Map.of(
                "pacienteId", pacienteUno,
                "profesionalId", profesional,
                "prestacionId", consulta,
                "inicio", inicio,
                "motivo", motivo))).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        return cuerpo(resultado);
    }

    private static JsonNode porId(JsonNode turnos, long id) {
        for (JsonNode turno : turnos) {
            if (turno.get("id").asLong() == id) {
                return turno;
            }
        }
        throw new AssertionError("No aparece el turno " + id + " en la agenda");
    }

    private static String agendaDelDia() {
        return TURNOS + "?desde=" + hora(0, 0) + "&hasta=" + hora(23, 59);
    }
}
