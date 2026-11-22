package ar.turnos.odontologia.agenda.disponibilidad;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.shared.security.Navegador;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Base de los tests de integración de C-07. Reutiliza los datos de la agenda (Dra. Ana Ejemplo, Dr. Bruno
 * Prueba, sillones, "Consulta", pacientes y usuarios) y suma el usuario {@code bruno.prueba}. A diferencia de la
 * base de la agenda, arranca sin franjas: cada escenario carga las suyas (design D11). Las fechas son de
 * diciembre de 2026 en Buenos Aires; el 2026-12-01 es martes.
 */
public abstract class IntegracionDisponibilidadBase extends IntegracionAgendaBase {

    @BeforeEach
    void prepararDisponibilidad() {
        jdbc.execute("TRUNCATE horario_trabajo, ausencia_profesional, bloqueo_agenda RESTART IDENTITY");
        jdbc.update("INSERT INTO usuario(username, password_hash, rol, profesional_id) "
                + "VALUES ('bruno.prueba',?,'ODONTOLOGO',?)", passwordEncoder.encode(clave), drB);
    }

    protected static String horarios(long profesional) {
        return "/api/v1/profesionales/" + profesional + "/horarios";
    }

    protected static String ausencias(long profesional) {
        return "/api/v1/profesionales/" + profesional + "/ausencias";
    }

    protected static String bloqueos(long profesional) {
        return "/api/v1/profesionales/" + profesional + "/bloqueos";
    }

    /** Fecha y hora de diciembre de 2026 en ISO-8601 con la zona de Buenos Aires. */
    protected static String diciembre(int dia, int hora, int minuto) {
        return LocalDateTime.of(2026, 12, dia, hora, minuto).atZone(BUENOS_AIRES)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    protected static Map<String, Object> franja(int dia, String desde, String hasta) {
        Map<String, Object> franja = new LinkedHashMap<>();
        franja.put("dia", dia);
        franja.put("desde", desde);
        franja.put("hasta", hasta);
        return franja;
    }

    /** Carga franjas por SQL, sin pasar por la API: es la preparación de un escenario. */
    protected void cargarFranja(long profesional, int dia, String desde, String hasta) {
        jdbc.update("INSERT INTO horario_trabajo(profesional_id, dia_semana, hora_desde, hora_hasta) "
                + "VALUES (?, ?, CAST(? AS TIME), CAST(? AS TIME))", profesional, dia, desde, hasta);
    }

    protected void cargarDeLunesAViernes(long profesional, String desde, String hasta) {
        for (int dia = 1; dia <= 5; dia++) {
            cargarFranja(profesional, dia, desde, hasta);
        }
    }

    protected MvcResult definirHorarios(Navegador navegador, long profesional, List<Map<String, Object>> franjas)
            throws Exception {
        return navegador.enviar(MockMvcRequestBuilders.put(horarios(profesional))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("franjas", franjas)))
                .header(Navegador.ENCABEZADO_CSRF, navegador.tokenCsrf())).andReturn();
    }

    protected MvcResult borrar(Navegador navegador, String url) throws Exception {
        return navegador.enviar(MockMvcRequestBuilders.delete(url)
                .header(Navegador.ENCABEZADO_CSRF, navegador.tokenCsrf())).andReturn();
    }

    protected MvcResult registrarAusencia(Navegador navegador, long profesional, String tipo, String inicio,
                                          String fin) throws Exception {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("tipo", tipo);
        pedido.put("inicio", inicio);
        pedido.put("fin", fin);
        pedido.put("motivo", "Motivo ficticio");
        return navegador.post(ausencias(profesional), json.writeValueAsString(pedido)).andReturn();
    }

    protected MvcResult bloquear(Navegador navegador, long profesional, String inicio, String fin) throws Exception {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("inicio", inicio);
        pedido.put("fin", fin);
        pedido.put("motivo", "Capacitación ficticia");
        return navegador.post(bloqueos(profesional), json.writeValueAsString(pedido)).andReturn();
    }

    /** Registra el período con {@code admin} o {@code recepcion} y devuelve su id (falla si no es 201). */
    protected long idCreado(MvcResult resultado) throws Exception {
        if (resultado.getResponse().getStatus() != 201) {
            throw new AssertionError("No se registró: " + resultado.getResponse().getContentAsString());
        }
        return cuerpo(resultado).path("periodo").path("id").asLong();
    }

    /** Las franjas de la respuesta como texto {@code "dia desde-hasta"}, para comparar con facilidad. */
    protected List<String> franjasDe(JsonNode respuesta) {
        List<String> franjas = new ArrayList<>();
        respuesta.path("franjas").forEach(f -> franjas.add(
                f.path("dia").asInt() + " " + f.path("desde").asText() + "-" + f.path("hasta").asText()));
        return franjas;
    }

    protected static int status(MvcResult resultado) {
        return resultado.getResponse().getStatus();
    }

    protected String codigo(MvcResult resultado) throws Exception {
        return cuerpo(resultado).path("codigo").asText();
    }
}
