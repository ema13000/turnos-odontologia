package ar.turnos.odontologia.notificaciones.api;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import ar.turnos.odontologia.notificaciones.infrastructure.ServicioNotificaciones;
import ar.turnos.odontologia.shared.security.Navegador;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Escenarios del spec {@code confirmacion-asistencia} (C-11) contra el endpoint público, con PostgreSQL real. "Paciente
 * Uno" (DNI 99000001) tiene un turno RESERVADO con la Dra. A (Ana Ejemplo) en el Sillón 1 el 2026-12-02 a las 07:30 y
 * recibió un recordatorio con un token.
 */
@Tag("integration")
class ConfirmacionApiIntegrationTest extends IntegracionNotificacionesBase {

    private static final String CONFIRMACIONES = "/api/v1/public/confirmaciones/";

    @Autowired ServicioNotificaciones servicio;

    private long turno;
    private String token;

    /** Deja el turno RESERVADO de "Paciente Uno" con su recordatorio enviado y guarda el token del link. */
    private void turnoConRecordatorio() {
        contacto(pacienteUno, true, true, "paciente.uno@example.com", "+54 11 5555-0001");
        turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        reservadoEn(turno, en(2026, 11, 28, 12, 0));
        servicio.enviarRecordatorios();
        token = envios.ultimoToken();
    }

    /** Como el frontend: primero pide el token CSRF a /public/sesion y después hace el POST. */
    private MvcResult confirmar(String queToken) throws Exception {
        Navegador navegador = navegador();
        navegador.get(SESION);
        return navegador.post(CONFIRMACIONES + queToken, null).andReturn();
    }

    private String estadoDelTurno() {
        return jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turno);
    }

    private int registrosConfirmar() {
        return jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE accion = 'CONFIRMAR'", Integer.class);
    }

    // ---------- Requirement: Confirmar la asistencia con el token del recordatorio ----------

    @Test
    @DisplayName("Confirmación con un token válido")
    void confirmacionConUnTokenValido() throws Exception {
        // GIVEN un turno RESERVADO de "Paciente Uno" con un recordatorio enviado y su token sin usar
        turnoConRecordatorio();

        // WHEN se pide POST /api/v1/public/confirmaciones/{token} sin sesión
        MvcResult resultado = confirmar(token);

        // THEN la respuesta es 200 AND el turno queda CONFIRMADO
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(estadoDelTurno()).isEqualTo("CONFIRMADO");
    }

    @Test
    @DisplayName("La respuesta solo trae lo necesario")
    void laRespuestaSoloTraeLoNecesario() throws Exception {
        // GIVEN el mismo turno y token
        turnoConRecordatorio();

        // WHEN se confirma
        MvcResult resultado = confirmar(token);

        // THEN la respuesta incluye la fecha, la hora, el nombre del profesional y el estado CONFIRMADO
        JsonNode cuerpo = cuerpo(resultado);
        assertThat(cuerpo.get("fecha").asText()).isEqualTo("2026-12-02");
        assertThat(cuerpo.get("hora").asText()).isEqualTo("07:30");
        assertThat(cuerpo.get("profesional").asText()).isEqualTo("Ana Ejemplo");
        assertThat(cuerpo.get("estado").asText()).isEqualTo("CONFIRMADO");
        // AND no incluye el DNI, el email, el teléfono, la prestación ni datos de otros pacientes
        List<String> campos = new ArrayList<>();
        cuerpo.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("fecha", "hora", "profesional", "estado");
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain(DNI_PACIENTE_UNO, "99000002", "@example.com", "5555", "Consulta", "Paciente", "Dos");
    }

    @Test
    void sinSesionAlcanzaYConLaSesionDeOtroRolTambienFunciona() throws Exception {
        turnoConRecordatorio();
        Navegador recepcion = como("recepcion");

        MvcResult resultado = recepcion.post(CONFIRMACIONES + token, null).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(estadoDelTurno()).isEqualTo("CONFIRMADO");
    }

    @Test
    void conLaSesionDelPropioPacienteTambienFunciona() throws Exception {
        turnoConRecordatorio();
        Navegador paciente = iniciarSesion("paciente.uno@example.com", clave);

        MvcResult resultado = paciente.post(CONFIRMACIONES + token, null).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    // ---------- Requirement: El token es de un solo uso ----------

    @Test
    @DisplayName("Segundo uso del mismo token")
    void segundoUsoDelMismoToken() throws Exception {
        // GIVEN un token que ya confirmó el turno
        turnoConRecordatorio();
        assertThat(confirmar(token).getResponse().getStatus()).isEqualTo(200);

        // WHEN se pide otra vez POST /api/v1/public/confirmaciones/{token}
        MvcResult segundo = confirmar(token);

        // THEN la respuesta es 409 con el código TOKEN_USADO
        assertThat(segundo.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpo(segundo).get("codigo").asText()).isEqualTo("TOKEN_USADO");
        // AND el turno sigue CONFIRMADO y no se crea un segundo registro de auditoría
        assertThat(estadoDelTurno()).isEqualTo("CONFIRMADO");
        assertThat(registrosConfirmar()).isEqualTo(1);
    }

    // ---------- Requirement: Tokens inexistentes y turnos que ya no se pueden confirmar ----------

    @Test
    @DisplayName("Token que no existe")
    void tokenQueNoExiste() throws Exception {
        turnoConRecordatorio();

        // WHEN se pide POST /api/v1/public/confirmaciones/{token} con un token que nunca se emitió
        MvcResult resultado = confirmar("token-que-nunca-se-emitio");

        // THEN la respuesta es 404 con el código TOKEN_INEXISTENTE AND no cambia ningún turno
        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado).get("codigo").asText()).isEqualTo("TOKEN_INEXISTENTE");
        assertThat(estadoDelTurno()).isEqualTo("RESERVADO");
        assertThat(registrosConfirmar()).isZero();
    }

    @Test
    @DisplayName("Turno cancelado después del recordatorio")
    void turnoCanceladoDespuesDelRecordatorio() throws Exception {
        // GIVEN un turno de "Paciente Uno" que se canceló después de enviarse el recordatorio, con su token sin usar
        turnoConRecordatorio();
        jdbc.update("UPDATE turno SET estado = 'CANCELADO' WHERE id = ?", turno);

        // WHEN se intenta confirmar con ese token
        MvcResult resultado = confirmar(token);

        // THEN la respuesta es 409 con el código TURNO_NO_CONFIRMABLE AND el turno sigue CANCELADO
        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpo(resultado).get("codigo").asText()).isEqualTo("TURNO_NO_CONFIRMABLE");
        assertThat(estadoDelTurno()).isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("Turno ya confirmado")
    void turnoYaConfirmado() throws Exception {
        // GIVEN un turno que ya está CONFIRMADO, con un token sin usar
        turnoConRecordatorio();
        jdbc.update("UPDATE turno SET estado = 'CONFIRMADO' WHERE id = ?", turno);

        // WHEN se intenta confirmar con ese token
        MvcResult resultado = confirmar(token);

        // THEN la respuesta es 409 con el código TURNO_NO_CONFIRMABLE
        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpo(resultado).get("codigo").asText()).isEqualTo("TURNO_NO_CONFIRMABLE");
    }

    @Test
    void unTokenConFormatoRaroSeTrataComoInexistenteSinErrorDelServidor() throws Exception {
        turnoConRecordatorio();

        MvcResult resultado = confirmar("a".repeat(5_000));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(resultado).get("codigo").asText()).isEqualTo("TOKEN_INEXISTENTE");
    }

    // ---------- Requirement: El token no se guarda en claro ----------

    @Test
    @DisplayName("El token no aparece en la base")
    void elTokenNoApareceEnLaBase() throws Exception {
        // GIVEN un recordatorio enviado con su link de confirmación
        turnoConRecordatorio();
        confirmar(token);

        // WHEN se consulta la tabla de notificaciones
        List<String> filas = jdbc.queryForList("SELECT row_to_json(n)::text FROM notificacion n", String.class);

        // THEN ningún valor guardado es igual al token del link (ni lo contiene)
        assertThat(filas).isNotEmpty().noneMatch(fila -> fila.contains(token));
        assertThat(jdbc.queryForObject("SELECT token_confirmacion FROM notificacion", String.class))
                .hasSize(64).isNotEqualTo(token);
    }

    // ---------- Requirement: Endpoint público con CSRF activo ----------

    @Test
    @DisplayName("Confirmación sin el token CSRF")
    void confirmacionSinElTokenCsrf() throws Exception {
        // GIVEN un token válido sin usar
        turnoConRecordatorio();

        // WHEN se pide POST /api/v1/public/confirmaciones/{token} sin el encabezado CSRF
        Navegador navegador = navegador();
        navegador.get(SESION);
        MvcResult resultado = navegador.postSinEncabezadoCsrf(CONFIRMACIONES + token, null).andReturn();

        // THEN la respuesta es 403 AND el turno sigue RESERVADO
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(estadoDelTurno()).isEqualTo("RESERVADO");
    }

    @Test
    @DisplayName("Confirmar con GET no cambia nada")
    void confirmarConGetNoCambiaNada() throws Exception {
        // GIVEN un token válido sin usar
        turnoConRecordatorio();

        // WHEN se pide GET /api/v1/public/confirmaciones/{token}
        MvcResult resultado = mvc.perform(MockMvcRequestBuilders.get(CONFIRMACIONES + token)).andReturn();

        // THEN el turno sigue RESERVADO y el token sigue sin usar (y el método no está permitido)
        assertThat(resultado.getResponse().getStatus()).isEqualTo(405);
        assertThat(estadoDelTurno()).isEqualTo("RESERVADO");
        assertThat(jdbc.queryForObject("SELECT usado_en IS NULL FROM notificacion", Boolean.class)).isTrue();
    }
}
