package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import ar.turnos.odontologia.notificaciones.application.EnviarRecordatorios.Resumen;
import ar.turnos.odontologia.notificaciones.application.TurnoConfirmado;
import ar.turnos.odontologia.notificaciones.domain.CodigoConfirmacion;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Casos de uso de punta a punta con PostgreSQL real y el reloj fijo del 2026-12-01 a las 08:00. La tarea programada
 * está apagada: el test llama al servicio a mano.
 */
@Tag("integration")
class ServicioNotificacionesIntegrationTest extends IntegracionNotificacionesBase {

    @Autowired ServicioNotificaciones servicio;
    @Autowired ApplicationContext contexto;

    /** "Paciente Uno" con consentimiento, WhatsApp y email, y su turno de mañana a las 07:30 reservado el 28/11. */
    private long turnoDePacienteUno() {
        contacto(pacienteUno, true, true, "paciente.uno@example.com", "+54 11 5555-0001");
        long turno = turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
        reservadoEn(turno, en(2026, 11, 28, 12, 0));
        return turno;
    }

    private String estadoDelTurno(long turno) {
        return jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turno);
    }

    @Test
    void enviaElRecordatorioYElPacienteConfirmaConElLinkDePuntaAPunta() {
        long turno = turnoDePacienteUno();

        Resumen resumen = servicio.enviarRecordatorios();

        assertThat(resumen.nuevas()).isEqualTo(1);
        assertThat(envios.whatsapp.enviados).hasSize(1);
        assertThat(envios.whatsapp.enviados.get(0).cuerpo())
                .contains("02/12/2026", "07:30", "Calle Ficticia 123",
                        "https://turnos.example.test/confirmar/")
                .doesNotContain("Consulta", "Paciente Uno", "Ana Ejemplo");
        assertThat(jdbc.queryForMap("SELECT canal, tipo, estado FROM notificacion WHERE turno_id = ?", turno))
                .containsEntry("canal", "WHATSAPP").containsEntry("tipo", "RECORDATORIO")
                .containsEntry("estado", "ENVIADA");

        TurnoConfirmado confirmado = servicio.confirmar(envios.ultimoToken());

        assertThat(confirmado.inicio()).isEqualTo(turnoDeManana());
        assertThat(confirmado.nombreProfesional()).isEqualTo("Ana Ejemplo");
        assertThat(estadoDelTurno(turno)).isEqualTo("CONFIRMADO");
        assertThat(jdbc.queryForObject("SELECT usado_en IS NOT NULL FROM notificacion WHERE turno_id = ?",
                Boolean.class, turno)).isTrue();
        assertThat(jdbc.queryForList("SELECT actor FROM turno_auditoria WHERE turno_id = ? AND accion = 'CONFIRMAR'",
                String.class, turno)).containsExactly("paciente-token");
    }

    @Test
    void dosCorridasSeguidasNoMandanDosRecordatorios() {
        long turno = turnoDePacienteUno();

        servicio.enviarRecordatorios();
        Resumen segunda = servicio.enviarRecordatorios();

        assertThat(segunda.nuevas()).isZero();
        assertThat(envios.totalEnviados()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion WHERE turno_id = ?", Integer.class, turno))
                .isEqualTo(1);
    }

    @Test
    void unaFallaDeEnvioQuedaRegistradaYNoDejaLaTransaccionAbierta() {
        long turno = turnoDePacienteUno();
        envios.whatsapp.falla = true;

        servicio.enviarRecordatorios();

        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(jdbc.queryForMap("SELECT estado, intentos, canal FROM notificacion WHERE turno_id = ?", turno))
                .containsEntry("estado", "FALLIDA").containsEntry("intentos", 1).containsEntry("canal", "WHATSAPP");
        // la base sigue respondiendo y el turno no cambió
        assertThat(estadoDelTurno(turno)).isEqualTo("RESERVADO");
        assertThat(envios.email.enviados).isEmpty();
    }

    @Test
    void tresFallasPorWhatsappPasanAEmailEnLaMismaFilaCuandoVenceElReintento() {
        long turno = turnoDePacienteUno();
        envios.whatsapp.falla = true;
        servicio.enviarRecordatorios();
        // el reintento venció hace un minuto y ya falló tres veces por WhatsApp
        jdbc.update("UPDATE notificacion SET intentos = 3, proximo_intento = ? WHERE turno_id = ?",
                Timestamp.from(en(2026, 12, 1, 7, 59)), turno);

        Resumen resumen = servicio.enviarRecordatorios();

        assertThat(resumen.reintentadas()).isEqualTo(1);
        assertThat(envios.email.enviados).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion WHERE turno_id = ?", Integer.class, turno))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT canal, estado, canal_alternativo, intentos FROM notificacion"
                + " WHERE turno_id = ?", turno))
                .containsEntry("canal", "EMAIL").containsEntry("estado", "ENVIADA")
                .containsEntry("canal_alternativo", true).containsEntry("intentos", 0);
        // el link del email lleva un token nuevo y confirma
        assertThat(servicio.confirmar(envios.ultimoToken()).nombreProfesional()).isEqualTo("Ana Ejemplo");
    }

    @Test
    void unTurnoCanceladoDespuesDelRecordatorioNoSeConfirmaYElTokenNoSeConsume() {
        long turno = turnoDePacienteUno();
        servicio.enviarRecordatorios();
        String token = envios.ultimoToken();
        jdbc.update("UPDATE turno SET estado = 'CANCELADO' WHERE id = ?", turno);

        assertThatThrownBy(() -> servicio.confirmar(token))
                .isInstanceOfSatisfying(ConfirmacionRechazadaException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoConfirmacion.TURNO_NO_CONFIRMABLE));

        assertThat(estadoDelTurno(turno)).isEqualTo("CANCELADO");
        assertThat(jdbc.queryForObject("SELECT usado_en IS NULL FROM notificacion WHERE turno_id = ?", Boolean.class,
                turno)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE accion = 'CONFIRMAR'",
                Integer.class)).isZero();
    }

    @Test
    void dosConfirmacionesSimultaneasConElMismoTokenConfirmanUnaSolaVez() throws Exception {
        long turno = turnoDePacienteUno();
        servicio.enviarRecordatorios();
        String token = envios.ultimoToken();
        int hilos = 6;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<String>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    try {
                        servicio.confirmar(token);
                        return "CONFIRMADO";
                    } catch (ConfirmacionRechazadaException e) {
                        return e.codigo().name();
                    }
                }));
            }
            salida.countDown();
            List<String> respuestas = new ArrayList<>();
            for (Future<String> resultado : resultados) {
                respuestas.add(resultado.get());
            }

            assertThat(respuestas).filteredOn("CONFIRMADO"::equals).hasSize(1);
            assertThat(respuestas).filteredOn(r -> !r.equals("CONFIRMADO")).containsOnly("TOKEN_USADO");
            assertThat(estadoDelTurno(turno)).isEqualTo("CONFIRMADO");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE turno_id = ? AND accion = 'CONFIRMAR'",
                    Integer.class, turno)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void avisaLaReasignacionPorElCanalDelPacienteSinLink() {
        long turno = turnoDePacienteUno();

        servicio.avisarReasignacion(turno);

        assertThat(envios.whatsapp.enviados).hasSize(1);
        assertThat(envios.whatsapp.enviados.get(0).cuerpo()).contains("reasignado", "02/12/2026", "07:30")
                .doesNotContain("http");
        assertThat(jdbc.queryForMap("SELECT tipo, estado, token_confirmacion FROM notificacion WHERE turno_id = ?", turno))
                .containsEntry("tipo", "REASIGNACION").containsEntry("estado", "ENVIADA")
                .containsEntry("token_confirmacion", null);
    }

    // ---------- Requirement: Recuperación de un envío interrumpido ----------

    @Test
    @DisplayName("Notificación pendiente que quedó sin enviar")
    void notificacionPendienteQueQuedoSinEnviar() {
        // GIVEN un corte entre el registro y el envío: el proceso murió con la notificación PENDIENTE en la base
        long turno = turnoDePacienteUno();
        envios.whatsapp.corte = true;
        assertThatThrownBy(() -> servicio.enviarRecordatorios()).isInstanceOf(Error.class);
        envios.whatsapp.corte = false;
        assertThat(estadoDeLaNotificacion(turno)).isEqualTo("PENDIENTE");
        assertThat(envios.totalEnviados()).isZero();
        // ... y pasaron más de 10 minutos desde que se registró
        jdbc.update("UPDATE notificacion SET programada_para = ? WHERE turno_id = ?",
                Timestamp.from(en(2026, 12, 1, 7, 49)), turno);

        // WHEN corre la tarea de recordatorios
        Resumen resumen = servicio.enviarRecordatorios();

        // THEN se envía el recordatorio y la notificación queda ENVIADA
        assertThat(resumen.reintentadas()).isEqualTo(1);
        assertThat(envios.whatsapp.enviados).hasSize(1);
        assertThat(estadoDeLaNotificacion(turno)).isEqualTo("ENVIADA");
        assertThat(servicio.confirmar(envios.ultimoToken()).inicio()).isEqualTo(turnoDeManana());
    }

    @Test
    @DisplayName("Notificación pendiente reciente")
    void notificacionPendienteReciente() {
        // GIVEN una notificación RECORDATORIO en estado PENDIENTE creada hace menos de 10 minutos
        long turno = turnoDePacienteUno();
        envios.whatsapp.corte = true;
        assertThatThrownBy(() -> servicio.enviarRecordatorios()).isInstanceOf(Error.class);
        envios.whatsapp.corte = false;
        jdbc.update("UPDATE notificacion SET programada_para = ? WHERE turno_id = ?",
                Timestamp.from(en(2026, 12, 1, 7, 51)), turno);

        // WHEN corre la tarea de recordatorios
        servicio.enviarRecordatorios();

        // THEN no se envía nada todavía
        assertThat(envios.totalEnviados()).isZero();
        assertThat(estadoDeLaNotificacion(turno)).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("Dos corridas simultáneas sobre la misma notificación")
    void dosCorridasSimultaneasSobreLaMismaNotificacion() throws Exception {
        // GIVEN una notificación FALLIDA con reintento vencido
        long turno = turnoDePacienteUno();
        envios.whatsapp.falla = true;
        servicio.enviarRecordatorios();
        envios.whatsapp.falla = false;
        jdbc.update("UPDATE notificacion SET proximo_intento = ? WHERE turno_id = ?",
                Timestamp.from(en(2026, 12, 1, 7, 59)), turno);

        // WHEN dos corridas de la tarea la procesan al mismo tiempo
        int hilos = 6;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Resumen>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return servicio.enviarRecordatorios();
                }));
            }
            salida.countDown();
            int reintentadas = 0;
            for (Future<Resumen> resultado : resultados) {
                reintentadas += resultado.get().reintentadas();
            }

            // THEN se envía una sola vez
            assertThat(reintentadas).isEqualTo(1);
            assertThat(envios.whatsapp.enviados).hasSize(1);
            assertThat(estadoDeLaNotificacion(turno)).isEqualTo("ENVIADA");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void unaPendienteViejaConElTurnoCanceladoYaIniciadoOSinConsentimientoNoSeReprocesa() {
        long cancelado = turnoDePacienteUno();
        contacto(pacienteDos, true, true, "paciente.dos@example.com", "+54 11 5555-0002");
        long iniciado = turno(pacienteDos, drB, sillon2, en(2026, 12, 1, 7, 30), "RESERVADO");
        long sinConsentimiento = turno(pacienteDos, drB, sillon2, en(2026, 12, 1, 18, 0), "RESERVADO");
        jdbc.update("UPDATE turno SET estado = 'CANCELADO' WHERE id = ?", cancelado);
        jdbc.update("UPDATE paciente SET consentimiento_comunicaciones = FALSE WHERE id = ?", pacienteDos);
        for (long t : new long[] {cancelado, iniciado, sinConsentimiento}) {
            jdbc.update("INSERT INTO notificacion(turno_id, canal, tipo, estado, programada_para)"
                    + " VALUES (?, 'WHATSAPP', 'RECORDATORIO', 'PENDIENTE', ?)", t, Timestamp.from(en(2026, 12, 1, 7, 0)));
        }

        servicio.enviarRecordatorios();

        assertThat(envios.totalEnviados()).isZero();
        assertThat(jdbc.queryForList("SELECT estado || ':' || coalesce(proximo_intento::text, 'sin') FROM notificacion",
                String.class)).containsOnly("FALLIDA:sin");
    }

    @Test
    void laReasignacionNuncaGuardaHashDeToken() {
        // un turno fuera de la ventana de 24 horas: solo interviene el aviso, no el recordatorio
        contacto(pacienteUno, true, true, "paciente.uno@example.com", "+54 11 5555-0001");
        long turno = turno(pacienteUno, draA, sillon1, en(2026, 12, 3, 10, 0), "RESERVADO");
        envios.whatsapp.falla = true;
        servicio.avisarReasignacion(turno);
        assertThat(tokenDeLaNotificacion(turno)).isNull();
        envios.whatsapp.falla = false;
        jdbc.update("UPDATE notificacion SET proximo_intento = ? WHERE turno_id = ?",
                Timestamp.from(en(2026, 12, 1, 7, 59)), turno);

        servicio.enviarRecordatorios();

        assertThat(envios.whatsapp.enviados).hasSize(1);
        assertThat(tokenDeLaNotificacion(turno)).isNull();
    }

    private String estadoDeLaNotificacion(long turno) {
        return jdbc.queryForObject("SELECT estado FROM notificacion WHERE turno_id = ? AND tipo = 'RECORDATORIO'",
                String.class, turno);
    }

    private String tokenDeLaNotificacion(long turno) {
        return jdbc.queryForObject("SELECT token_confirmacion FROM notificacion WHERE turno_id = ?", String.class, turno);
    }

    @Test
    void conLaTareaApagadaNadaCorreSolo() {
        turnoDePacienteUno();

        assertThat(contexto.getBeansOfType(TareaRecordatorios.class)).isEmpty();
        assertThat(contexto.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
        assertThat(envios.totalEnviados()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion", Integer.class)).isZero();
    }
}
