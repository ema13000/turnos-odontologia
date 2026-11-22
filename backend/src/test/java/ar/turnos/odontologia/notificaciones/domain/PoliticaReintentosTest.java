package ar.turnos.odontologia.notificaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Política de reintentos del spec {@code notificaciones-recordatorios} (C-11, design D6). */
class PoliticaReintentosTest {

    @Test
    void esperaDe5Luego10YLuego20Minutos() {
        assertThat(PoliticaReintentos.espera(1)).isEqualTo(Duration.ofMinutes(5));
        assertThat(PoliticaReintentos.espera(2)).isEqualTo(Duration.ofMinutes(10));
        assertThat(PoliticaReintentos.espera(3)).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void laEsperaSoloExisteParaLasFallas1A3() {
        assertThatThrownBy(() -> PoliticaReintentos.espera(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PoliticaReintentos.espera(4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elTopeEsDeTresIntentosPorCanal() {
        assertThat(PoliticaReintentos.MAX_INTENTOS).isEqualTo(3);
        assertThat(PoliticaReintentos.haySiguienteIntento(Canal.WHATSAPP, 1, false)).isTrue();
        assertThat(PoliticaReintentos.haySiguienteIntento(Canal.WHATSAPP, 2, false)).isTrue();
        assertThat(PoliticaReintentos.haySiguienteIntento(Canal.EMAIL, 2, false)).isTrue();
    }

    @Test
    void tresFallasPorWhatsappConEmailPasanAEmail() {
        assertThat(PoliticaReintentos.cambiaAEmail(Canal.WHATSAPP, 3, true)).isTrue();
        assertThat(PoliticaReintentos.haySiguienteIntento(Canal.WHATSAPP, 3, true)).isTrue();
        // todavía no se agotaron los intentos por WhatsApp
        assertThat(PoliticaReintentos.cambiaAEmail(Canal.WHATSAPP, 2, true)).isFalse();
    }

    @Test
    void tresFallasPorWhatsappSinEmailEsTerminal() {
        assertThat(PoliticaReintentos.cambiaAEmail(Canal.WHATSAPP, 3, false)).isFalse();
        assertThat(PoliticaReintentos.haySiguienteIntento(Canal.WHATSAPP, 3, false)).isFalse();
    }

    @Test
    void elEmailNoVuelveAWhatsapp() {
        assertThat(PoliticaReintentos.cambiaAEmail(Canal.EMAIL, 3, true)).isFalse();
        assertThat(PoliticaReintentos.haySiguienteIntento(Canal.EMAIL, 3, true)).isFalse();
    }
}
