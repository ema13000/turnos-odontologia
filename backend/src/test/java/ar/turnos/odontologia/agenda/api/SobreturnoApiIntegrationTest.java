package ar.turnos.odontologia.agenda.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.shared.security.Navegador;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Un test por escenario del delta {@code agenda-api} correspondiente a los sobreturnos (C-08). */
@Tag("integration")
class SobreturnoApiIntegrationTest extends IntegracionAgendaBase {

    private static final String MOTIVO = "Urgencia administrativa ficticia";

    @Test
    @DisplayName("Recepción crea un sobreturno")
    void recepcionCreaUnSobreturno() throws Exception {
        // GIVEN recepcion con sesión iniciada y los dos sillones activos y libres
        Navegador recepcion = como("recepcion");

        // WHEN pide POST /api/v1/turnos/sobreturno para "Paciente Uno" con la Dra. A, "Consulta",
        // inicio 2026-12-01T10:00:00-03:00 y un motivo administrativo
        MvcResult resultado = crearSobreturno(recepcion, pacienteUno, draA, hora(10, 0), MOTIVO);

        // THEN la respuesta es 201 con estado RESERVADO, fin 10:30, esSobreturno verdadero y numeroReserva
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        JsonNode turno = cuerpo(resultado);
        assertThat(turno.get("estado").asText()).isEqualTo("RESERVADO");
        assertThat(turno.get("fin").asText()).isEqualTo(hora(10, 30));
        assertThat(turno.get("esSobreturno").asBoolean()).isTrue();
        assertThat(turno.get("numeroReserva").asText()).isNotBlank();
        // AND el cuerpo incluye el sillonId asignado por el servidor
        assertThat(turno.get("sillonId").asLong()).isIn(sillon1, sillon2);
        assertThat(turno.has("motivo")).isFalse();
    }

    @Test
    @DisplayName("El pedido no declara sillonId")
    void elPedidoNoDeclaraSillonId() throws Exception {
        // GIVEN recepcion con sesión iniciada
        como("recepcion");

        // WHEN revisa el contrato del pedido de sobreturno
        var componentes = Class.forName("ar.turnos.odontologia.agenda.api.CrearSobreturnoRequest")
                .getRecordComponents();
        String[] nombres = Arrays.stream(componentes).map(c -> c.getName()).toArray(String[]::new);

        // THEN no existe el campo sillonId, ni siquiera como campo opcional
        assertThat(nombres).doesNotContain("sillonId");
        // AND el sillón se asigna en el servidor según las reglas de agenda-turnos
        assertThat(nombres).contains("pacienteId", "profesionalId", "prestacionId", "inicio", "motivo");
        JsonNode turno = cuerpo(crearSobreturno(como("recepcion"), pacienteUno, draA, hora(10, 0), MOTIVO));
        assertThat(turno.get("sillonId").asLong()).isPositive();
    }

    @Test
    @DisplayName("Sobreturno sin motivo")
    void sobreturnoSinMotivo() throws Exception {
        // GIVEN recepcion con sesión iniciada
        Navegador recepcion = como("recepcion");

        // WHEN pide POST /api/v1/turnos/sobreturno sin motivo o con "   "
        MvcResult sinMotivo = crearSobreturno(recepcion, pacienteUno, draA, hora(10, 0), null);
        MvcResult soloEspacios = crearSobreturno(recepcion, pacienteUno, draA, hora(10, 0), "   ");

        // THEN la respuesta es 422 con el código DATOS_INVALIDOS
        assertProblema(sinMotivo, 422, "DATOS_INVALIDOS");
        assertProblema(soloEspacios, 422, "DATOS_INVALIDOS");
        // AND la lista de errores incluye el campo motivo
        assertThat(camposConError(sinMotivo)).contains("motivo");
        assertThat(camposConError(soloEspacios)).contains("motivo");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isZero();
    }

    @Test
    @DisplayName("La respuesta no expone el motivo")
    void laRespuestaNoExponeElMotivo() throws Exception {
        // GIVEN un sobreturno creado con un motivo administrativo
        MvcResult creacion = crearSobreturno(como("recepcion"), pacienteUno, draA, hora(10, 0), MOTIVO);
        JsonNode turno = cuerpo(creacion);

        // WHEN el personal consulta GET /api/v1/turnos o recibe la respuesta de creación
        MvcResult agenda = como("recepcion").get(agendaDelDia()).andReturn();

        // THEN ninguno de los cuerpos incluye el motivo
        assertThat(creacion.getResponse().getContentAsString()).doesNotContain(MOTIVO);
        assertThat(agenda.getResponse().getContentAsString()).doesNotContain(MOTIVO);
        assertThat(cuerpo(agenda)).hasSize(1);
        assertThat(turno.get("esSobreturno").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Un odontólogo crea un sobreturno")
    void unOdontologoCreaUnSobreturno() throws Exception {
        // GIVEN ana.ejemplo con sesión iniciada
        // WHEN pide POST /api/v1/turnos/sobreturno con datos válidos y un sillón libre
        MvcResult resultado = crearSobreturno(como("ana.ejemplo"), pacienteUno, draA, hora(10, 0), MOTIVO);

        // THEN la respuesta es 201
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("Un paciente intenta crear un sobreturno")
    void unPacienteIntentaCrearUnSobreturno() throws Exception {
        // GIVEN "Paciente Uno" con sesión iniciada
        // WHEN pide POST /api/v1/turnos/sobreturno con datos válidos
        MvcResult resultado = crearSobreturno(como("paciente.uno@example.com"), pacienteUno, draA,
                hora(10, 0), MOTIVO);

        // THEN la respuesta es 403 con el código ACCESO_DENEGADO
        assertProblema(resultado, 403, "ACCESO_DENEGADO");
        // AND no se crea ningún turno
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isZero();
    }

    // ---------- Triangulación ----------

    @Test
    @DisplayName("El motivo de 501 caracteres se rechaza con 422")
    void motivoDe501Caracteres() throws Exception {
        MvcResult resultado = crearSobreturno(como("recepcion"), pacienteUno, draA, hora(10, 0), "a".repeat(501));

        assertProblema(resultado, 422, "DATOS_INVALIDOS");
        assertThat(camposConError(resultado)).contains("motivo");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Un paciente inexistente se rechaza con 422")
    void pacienteInexistente() throws Exception {
        MvcResult resultado = crearSobreturno(como("recepcion"), 999999L, draA, hora(10, 0), MOTIVO);

        assertProblema(resultado, 422, "DATOS_INVALIDOS");
        assertThat(camposConError(resultado)).contains("pacienteId");
    }

    @Test
    @DisplayName("Un pedido sin CSRF se rechaza con 403")
    void sinCsrf() throws Exception {
        Navegador recepcion = como("recepcion");
        MvcResult resultado = recepcion.postSinEncabezadoCsrf(TURNOS + "/sobreturno",
                json.writeValueAsString(pedido(pacienteUno, draA, hora(10, 0), MOTIVO))).andReturn();

        assertProblema(resultado, 403, "CSRF_INVALIDO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isZero();
    }

    // ---------- Ayudas ----------

    private MvcResult crearSobreturno(Navegador navegador, long paciente, long profesional, String inicio,
                                      String motivo) throws Exception {
        return navegador.post(TURNOS + "/sobreturno", json.writeValueAsString(pedido(paciente, profesional,
                inicio, motivo))).andReturn();
    }

    private Map<String, Object> pedido(long paciente, long profesional, String inicio, String motivo) {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("pacienteId", paciente);
        pedido.put("profesionalId", profesional);
        pedido.put("prestacionId", consulta);
        pedido.put("inicio", inicio);
        if (motivo != null) {
            pedido.put("motivo", motivo);
        }
        return pedido;
    }

    private static String agendaDelDia() {
        return TURNOS + "?desde=" + hora(0, 0) + "&hasta=" + hora(23, 59);
    }

    private void assertProblema(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus())
                .as(resultado.getResponse().getContentAsString())
                .isEqualTo(estado);
        assertThat(cuerpo(resultado).get("codigo").asText()).isEqualTo(codigo);
    }

    private java.util.List<String> camposConError(MvcResult resultado) throws Exception {
        java.util.List<String> campos = new java.util.ArrayList<>();
        cuerpo(resultado).get("errores").forEach(e -> campos.add(e.get("campo").asText()));
        return campos;
    }
}
