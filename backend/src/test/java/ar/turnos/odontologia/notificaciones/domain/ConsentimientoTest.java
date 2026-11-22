package ar.turnos.odontologia.notificaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Regla "Solo con consentimiento" (RN-NO-03): sin consentimiento no hay canal, tenga los contactos que tenga. */
class ConsentimientoTest {

    @Test
    void sinConsentimientoNoHayCanalAunqueTengaWhatsappYEmail() {
        ContactoPaciente sinConsentimiento = new ContactoPaciente("paciente.uno@example.com", "+54 11 5555-0001", true, false);

        assertThat(sinConsentimiento.puedeRecibirComunicaciones()).isFalse();
        assertThat(sinConsentimiento.canalDeEnvio()).isEmpty();
    }

    @Test
    void conConsentimientoYUnContactoPuedeRecibir() {
        ContactoPaciente conConsentimiento = new ContactoPaciente("paciente.uno@example.com", null, false, true);

        assertThat(conConsentimiento.puedeRecibirComunicaciones()).isTrue();
        assertThat(conConsentimiento.canalDeEnvio()).isPresent();
    }

    @Test
    void conConsentimientoPeroSinContactoNoPuedeRecibir() {
        ContactoPaciente sinContacto = new ContactoPaciente(null, null, false, true);

        assertThat(sinContacto.puedeRecibirComunicaciones()).isFalse();
    }

    @Test
    void elContactoNoExponeElEmailNiElTelefonoAlImprimirse() {
        ContactoPaciente contacto = new ContactoPaciente("paciente.uno@example.com", "+54 11 5555-0001", true, true);

        assertThat(contacto.toString()).doesNotContain("paciente.uno", "example.com", "5555");
    }
}
