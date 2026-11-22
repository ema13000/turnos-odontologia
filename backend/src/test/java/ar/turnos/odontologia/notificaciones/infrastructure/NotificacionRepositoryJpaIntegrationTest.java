package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import ar.turnos.odontologia.notificaciones.application.HuellaToken;
import ar.turnos.odontologia.notificaciones.application.NotificacionRepository;
import ar.turnos.odontologia.notificaciones.application.NotificacionRepository.Reintento;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ar.turnos.odontologia.notificaciones.domain.EstadoNotificacion;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import ar.turnos.odontologia.notificaciones.domain.TipoNotificacion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Adaptador de {@link NotificacionRepository} contra PostgreSQL real, con datos ficticios. */
@Tag("integration")
class NotificacionRepositoryJpaIntegrationTest extends IntegracionNotificacionesBase {

    private static final Instant AHORA = en(2026, 12, 1, 8, 0);

    @Autowired NotificacionRepository repositorio;

    private long turnoDeUno() {
        contacto(pacienteUno, true, true, "paciente.uno@example.com", "+54 11 5555-0001");
        return turno(pacienteUno, draA, sillon1, turnoDeManana(), "RESERVADO");
    }

    private Notificacion recordatorio(long turnoId, String token) {
        return Notificacion.pendiente(turnoId, Canal.WHATSAPP, TipoNotificacion.RECORDATORIO, AHORA,
                HuellaToken.de(token));
    }

    @Test
    void insertaElRecordatorioPendienteYDevuelveSuId() {
        long turno = turnoDeUno();

        Optional<Notificacion> insertada = repositorio.insertarRecordatorio(recordatorio(turno, "token-1"));

        assertThat(insertada).isPresent();
        assertThat(insertada.get().id()).isNotNull();
        assertThat(jdbc.queryForMap("SELECT canal, tipo, estado, intentos, canal_alternativo, usado_en"
                + " FROM notificacion WHERE id = ?", insertada.get().id()))
                .containsEntry("canal", "WHATSAPP").containsEntry("tipo", "RECORDATORIO")
                .containsEntry("estado", "PENDIENTE").containsEntry("intentos", 0)
                .containsEntry("canal_alternativo", false).containsEntry("usado_en", null);
    }

    @Test
    void unSegundoRecordatorioDelMismoTurnoNoSeInsertaYNoAbortaLaTransaccion() {
        long turno = turnoDeUno();
        repositorio.insertarRecordatorio(recordatorio(turno, "token-1"));

        Optional<Notificacion> segunda = repositorio.insertarRecordatorio(recordatorio(turno, "token-2"));

        assertThat(segunda).isEmpty();
        assertThat(repositorio.existeRecordatorio(turno)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion", Integer.class)).isEqualTo(1);
    }

    @Test
    void dosInsercionesSimultaneasDelRecordatorioDejanUnaSolaFila() throws Exception {
        long turno = turnoDeUno();
        int hilos = 8;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Optional<Notificacion>>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                String token = "token-concurrente-" + i;
                Callable<Optional<Notificacion>> insercion = () -> {
                    salida.await();
                    return repositorio.insertarRecordatorio(recordatorio(turno, token));
                };
                resultados.add(pool.submit(insercion));
            }
            salida.countDown();
            int ganadores = 0;
            for (Future<Optional<Notificacion>> resultado : resultados) {
                ganadores += resultado.get().isPresent() ? 1 : 0;
            }

            assertThat(ganadores).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion WHERE turno_id = ?", Integer.class, turno))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lasReasignacionesNoTienenLimiteUnoPorTurno() {
        long turno = turnoDeUno();
        Notificacion aviso = Notificacion.pendiente(turno, Canal.EMAIL, TipoNotificacion.REASIGNACION, AHORA, null);

        repositorio.insertar(aviso);
        repositorio.insertar(aviso);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacion WHERE tipo = 'REASIGNACION'", Integer.class))
                .isEqualTo(2);
        assertThat(repositorio.existeRecordatorio(turno)).isFalse();
    }

    @Test
    void actualizaEstadoCanalIntentosYHuella() {
        long turno = turnoDeUno();
        Notificacion insertada = repositorio.insertarRecordatorio(recordatorio(turno, "token-1")).orElseThrow();
        Notificacion tercera = insertada.fallida(AHORA, true).fallida(AHORA, true).fallida(AHORA, true);

        repositorio.actualizar(tercera.aEmail().conHuellaDeToken(HuellaToken.de("token-nuevo")).fallida(AHORA, true));

        assertThat(jdbc.queryForMap("SELECT canal, estado, intentos, canal_alternativo, token_confirmacion,"
                + " proximo_intento IS NOT NULL AS hay_proximo FROM notificacion WHERE id = ?", insertada.id()))
                .containsEntry("canal", "EMAIL").containsEntry("estado", "FALLIDA").containsEntry("intentos", 1)
                .containsEntry("canal_alternativo", true).containsEntry("hay_proximo", true)
                .containsEntry("token_confirmacion", HuellaToken.de("token-nuevo"));
    }

    @Test
    void guardaSoloLaHuellaDelTokenNuncaElToken() {
        long turno = turnoDeUno();
        repositorio.insertarRecordatorio(recordatorio(turno, "token-secreto-ficticio"));

        assertThat(jdbc.queryForList("SELECT token_confirmacion FROM notificacion", String.class))
                .containsExactly(HuellaToken.de("token-secreto-ficticio"))
                .doesNotContain("token-secreto-ficticio");
        assertThat(repositorio.buscarPorHuella(HuellaToken.de("token-secreto-ficticio"))).isPresent();
        assertThat(repositorio.buscarPorHuella(HuellaToken.de("otro"))).isEmpty();
    }

    @Test
    void consumeElTokenUnaSolaVez() {
        long turno = turnoDeUno();
        repositorio.insertarRecordatorio(recordatorio(turno, "token-1"));

        assertThat(repositorio.consumirToken(HuellaToken.de("token-1"), AHORA)).isTrue();
        assertThat(repositorio.consumirToken(HuellaToken.de("token-1"), AHORA)).isFalse();
        assertThat(repositorio.consumirToken(HuellaToken.de("inexistente"), AHORA)).isFalse();
        assertThat(repositorio.buscarPorHuella(HuellaToken.de("token-1")).orElseThrow().usadoEn()).isEqualTo(AHORA);
    }

    @Test
    void dosUsosSimultaneosDelMismoTokenLoConsumenUnaSolaVez() throws Exception {
        long turno = turnoDeUno();
        repositorio.insertarRecordatorio(recordatorio(turno, "token-1"));
        int hilos = 8;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return repositorio.consumirToken(HuellaToken.de("token-1"), AHORA);
                }));
            }
            salida.countDown();
            int consumos = 0;
            for (Future<Boolean> resultado : resultados) {
                consumos += resultado.get() ? 1 : 0;
            }

            assertThat(consumos).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void devuelveLosReintentosVencidosConLosDatosDeSuTurno() {
        long turno = turnoDeUno();
        Notificacion fallida = repositorio.insertarRecordatorio(recordatorio(turno, "token-1")).orElseThrow()
                .fallida(AHORA, true);
        repositorio.actualizar(fallida);

        assertThat(repositorio.reintentosVencidos(AHORA.plus(Duration.ofMinutes(4)))).isEmpty();
        List<Reintento> vencidos = repositorio.reintentosVencidos(AHORA.plus(Duration.ofMinutes(5)));

        assertThat(vencidos).hasSize(1);
        assertThat(vencidos.get(0).notificacion().intentos()).isEqualTo(1);
        assertThat(vencidos.get(0).turno().turnoId()).isEqualTo(turno);
        assertThat(vencidos.get(0).turno().contacto().tieneEmail()).isTrue();
        assertThat(vencidos.get(0).turno().activo()).isTrue();
    }

    @Test
    void devuelveLasPendientesDeMasDe10MinutosYNoLasRecientes() {
        long turno = turnoDeUno();
        long otro = turno(pacienteDos, drB, sillon2, turnoDeManana(), "RESERVADO");
        Notificacion vieja = repositorio.insertarRecordatorio(Notificacion.pendiente(turno, Canal.WHATSAPP,
                TipoNotificacion.RECORDATORIO, AHORA.minus(Duration.ofMinutes(11)), HuellaToken.de("vieja"))).orElseThrow();
        repositorio.insertarRecordatorio(Notificacion.pendiente(otro, Canal.WHATSAPP,
                TipoNotificacion.RECORDATORIO, AHORA.minus(Duration.ofMinutes(9)), HuellaToken.de("reciente")));

        List<Reintento> vencidos = repositorio.reintentosVencidos(AHORA);

        assertThat(vencidos).extracting(r -> r.notificacion().id()).containsExactly(vieja.id());
    }

    @Test
    void reclamaLaFilaUnaSolaVezYLaDejaPendienteConLaReservaDeLaCorrida() {
        long turno = turnoDeUno();
        Notificacion fallida = repositorio.insertarRecordatorio(recordatorio(turno, "token-1")).orElseThrow()
                .fallida(AHORA.minus(Duration.ofMinutes(10)), true);
        repositorio.actualizar(fallida);

        assertThat(repositorio.reclamar(fallida, AHORA)).isTrue();
        assertThat(repositorio.reclamar(fallida, AHORA)).isFalse();
        assertThat(jdbc.queryForMap("SELECT estado, proximo_intento, programada_para FROM notificacion WHERE id = ?",
                fallida.id())).containsEntry("estado", "PENDIENTE").containsEntry("proximo_intento", null);
        // la reserva dura 10 minutos: pasado ese tiempo una corrida puede recuperarla
        assertThat(repositorio.reintentosVencidos(AHORA.plus(Duration.ofMinutes(9)))).isEmpty();
        assertThat(repositorio.reintentosVencidos(AHORA.plus(Duration.ofMinutes(10)))).hasSize(1);
    }

    @Test
    void noReclamaLoQueNoEstaVencido() {
        long turno = turnoDeUno();
        Notificacion reciente = repositorio.insertarRecordatorio(recordatorio(turno, "token-1")).orElseThrow();

        assertThat(repositorio.reclamar(reciente, AHORA)).isFalse();
    }

    @Test
    void variosHilosReclamandoLaMismaFilaVenciadaSoloUnoLaTiene() throws Exception {
        long turno = turnoDeUno();
        Notificacion fallida = repositorio.insertarRecordatorio(recordatorio(turno, "token-1")).orElseThrow()
                .fallida(AHORA.minus(Duration.ofMinutes(10)), true);
        repositorio.actualizar(fallida);
        int hilos = 8;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return repositorio.reclamar(fallida, AHORA);
                }));
            }
            salida.countDown();
            int ganadores = 0;
            for (Future<Boolean> resultado : resultados) {
                ganadores += resultado.get() ? 1 : 0;
            }

            assertThat(ganadores).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void noDevuelveComoReintentoLoEnviadoNiLoAbandonado() {
        long turno = turnoDeUno();
        long otro = turno(pacienteDos, drB, sillon2, turnoDeManana(), "RESERVADO");
        Notificacion enviada = repositorio.insertarRecordatorio(recordatorio(turno, "token-1")).orElseThrow();
        repositorio.actualizar(enviada.enviada(AHORA));
        Notificacion abandonada = repositorio.insertarRecordatorio(recordatorio(otro, "token-2")).orElseThrow();
        repositorio.actualizar(abandonada.fallida(AHORA, true).abandonada());

        assertThat(repositorio.reintentosVencidos(AHORA.plus(Duration.ofDays(1)))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT estado FROM notificacion WHERE id = ?", String.class, abandonada.id()))
                .isEqualTo(EstadoNotificacion.FALLIDA.name());
    }
}
