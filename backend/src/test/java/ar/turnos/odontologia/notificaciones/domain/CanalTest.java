package ar.turnos.odontologia.notificaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Escenarios de "Elección del canal" del spec {@code notificaciones-recordatorios} (C-11), con datos ficticios. */
class CanalTest {

    private static ContactoPaciente contacto(boolean whatsapp, String telefono, String email) {
        return new ContactoPaciente(email, telefono, whatsapp, true);
    }

    @Test
    @DisplayName("Paciente con WhatsApp")
    void pacienteConWhatsapp() {
        // GIVEN "Paciente Uno" con consentimiento, WhatsApp, teléfono y email
        ContactoPaciente uno = contacto(true, "+54 11 5555-0001", "paciente.uno@example.com");

        // WHEN se elige el canal de su recordatorio
        // THEN el canal es WHATSAPP
        assertThat(uno.canalDeEnvio()).contains(Canal.WHATSAPP);
    }

    @Test
    @DisplayName("Paciente sin WhatsApp")
    void pacienteSinWhatsapp() {
        // GIVEN "Paciente Uno" con consentimiento, sin WhatsApp y con email
        ContactoPaciente uno = contacto(false, "+54 11 5555-0001", "paciente.uno@example.com");

        // WHEN se elige el canal de su recordatorio
        // THEN el canal es EMAIL
        assertThat(uno.canalDeEnvio()).contains(Canal.EMAIL);
    }

    @Test
    void whatsappSinTelefonoCaeAEmail() {
        assertThat(contacto(true, null, "paciente.uno@example.com").canalDeEnvio()).contains(Canal.EMAIL);
        assertThat(contacto(true, "   ", "paciente.uno@example.com").canalDeEnvio()).contains(Canal.EMAIL);
    }

    @Test
    void emailVacioSeTrataComoAusente() {
        assertThat(contacto(false, "+54 11 5555-0001", "").canalDeEnvio()).isEqualTo(Optional.empty());
        assertThat(contacto(false, null, "   ").canalDeEnvio()).isEqualTo(Optional.empty());
        assertThat(contacto(false, null, null).canalDeEnvio()).isEqualTo(Optional.empty());
        assertThat(contacto(false, null, "   ").tieneEmail()).isFalse();
    }

    @Test
    void whatsappSinTelefonoYSinEmailNoTieneCanal() {
        assertThat(contacto(true, null, null).canalDeEnvio()).isEmpty();
    }

    @Test
    void elDestinoEsElTelefonoOElEmailSegunElCanal() {
        ContactoPaciente uno = contacto(true, "+54 11 5555-0001", "paciente.uno@example.com");

        assertThat(uno.destino(Canal.WHATSAPP)).isEqualTo("+54 11 5555-0001");
        assertThat(uno.destino(Canal.EMAIL)).isEqualTo("paciente.uno@example.com");
    }
}
