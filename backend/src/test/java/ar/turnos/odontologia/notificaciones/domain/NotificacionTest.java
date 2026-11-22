package ar.turnos.odontologia.notificaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Transiciones de {@link Notificacion}: enviada, fallida con espera creciente y respaldo por email (design D6). */
class NotificacionTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");

    private static Notificacion pendiente(Canal canal) {
        return Notificacion.pendiente(7L, canal, TipoNotificacion.RECORDATORIO, AHORA, "huella").conId(1L);
    }

    @Test
    void unaNotificacionNuevaEstaPendienteSinIntentos() {
        Notificacion n = pendiente(Canal.WHATSAPP);

        assertThat(n.estado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(n.intentos()).isZero();
        assertThat(n.canalAlternativo()).isFalse();
        assertThat(n.proximoIntento()).isNull();
        assertThat(n.usadoEn()).isNull();
    }

    @Test
    void enviadaGuardaElMomentoYNoTieneProximoIntento() {
        Notificacion enviada = pendiente(Canal.WHATSAPP).enviada(AHORA);

        assertThat(enviada.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(enviada.enviadaEn()).isEqualTo(AHORA);
        assertThat(enviada.proximoIntento()).isNull();
    }

    @Test
    void lasFallasEsperan5Luego10YLuego20Minutos() {
        Notificacion primera = pendiente(Canal.WHATSAPP).fallida(AHORA, true);
        assertThat(primera.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(primera.intentos()).isEqualTo(1);
        assertThat(primera.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(5)));

        Notificacion segunda = primera.fallida(AHORA, true);
        assertThat(segunda.intentos()).isEqualTo(2);
        assertThat(segunda.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(10)));

        Notificacion tercera = segunda.fallida(AHORA, true);
        assertThat(tercera.intentos()).isEqualTo(3);
        assertThat(tercera.proximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(20)));
        assertThat(tercera.canal()).isEqualTo(Canal.WHATSAPP);
    }

    @Test
    void despuesDeTresFallasPorWhatsappConEmailPasaAEmailConLosIntentosEnCero() {
        Notificacion tercera = pendiente(Canal.WHATSAPP).fallida(AHORA, true).fallida(AHORA, true).fallida(AHORA, true);
        assertThat(tercera.debeCambiarAEmail(true)).isTrue();

        Notificacion porEmail = tercera.aEmail();

        assertThat(porEmail.canal()).isEqualTo(Canal.EMAIL);
        assertThat(porEmail.canalAlternativo()).isTrue();
        assertThat(porEmail.intentos()).isZero();
        assertThat(porEmail.estado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(porEmail.proximoIntento()).isNull();
        assertThat(porEmail.turnoId()).isEqualTo(tercera.turnoId());
    }

    @Test
    void tresFallasPorWhatsappSinEmailTerminanFallidasSinProximoIntento() {
        Notificacion tercera = pendiente(Canal.WHATSAPP).fallida(AHORA, false).fallida(AHORA, false).fallida(AHORA, false);

        assertThat(tercera.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(tercera.proximoIntento()).isNull();
        assertThat(tercera.debeCambiarAEmail(false)).isFalse();
    }

    @Test
    void elEmailNoVuelveAWhatsappYTerminaTrasTresFallas() {
        Notificacion tercera = pendiente(Canal.EMAIL).fallida(AHORA, true).fallida(AHORA, true).fallida(AHORA, true);

        assertThat(tercera.canal()).isEqualTo(Canal.EMAIL);
        assertThat(tercera.proximoIntento()).isNull();
        assertThat(tercera.debeCambiarAEmail(true)).isFalse();
    }

    @Test
    void elReintentoVenceCuandoLlegaElMomentoProgramado() {
        Notificacion fallida = pendiente(Canal.WHATSAPP).fallida(AHORA, true);

        assertThat(fallida.reintentoVencido(AHORA.plus(Duration.ofMinutes(4)))).isFalse();
        assertThat(fallida.reintentoVencido(AHORA.plus(Duration.ofMinutes(5)))).isTrue();
        assertThat(pendiente(Canal.WHATSAPP).enviada(AHORA).reintentoVencido(AHORA.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void abandonadaQuedaFallidaSinProximoIntento() {
        Notificacion abandonada = pendiente(Canal.WHATSAPP).fallida(AHORA, true).abandonada();

        assertThat(abandonada.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(abandonada.proximoIntento()).isNull();
        assertThat(abandonada.reintentoVencido(AHORA.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void lasTransicionesNoModificanLaNotificacionOriginal() {
        Notificacion original = pendiente(Canal.WHATSAPP);

        original.fallida(AHORA, true);
        original.enviada(AHORA);

        assertThat(original.estado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(original.intentos()).isZero();
    }
}
