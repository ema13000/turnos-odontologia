package ar.turnos.odontologia.notificaciones.domain;

import java.util.Optional;

/**
 * Datos de comunicación de un paciente y regla que elige el canal (RN-NO-02, RN-NO-03): sin consentimiento no se
 * envía nada; con WhatsApp y teléfono, por WhatsApp; si no, por email si tiene; si no tiene ninguno, no se envía.
 * Un valor vacío o en blanco se trata como ausente.
 */
public record ContactoPaciente(String email, String telefono, boolean tieneWhatsapp, boolean consentimiento) {

    public Optional<Canal> canalDeEnvio() {
        if (!consentimiento) {
            return Optional.empty();
        }
        if (tieneWhatsapp && hayTexto(telefono)) {
            return Optional.of(Canal.WHATSAPP);
        }
        if (tieneEmail()) {
            return Optional.of(Canal.EMAIL);
        }
        return Optional.empty();
    }

    public boolean puedeRecibirComunicaciones() {
        return canalDeEnvio().isPresent();
    }

    public boolean tieneEmail() {
        return hayTexto(email);
    }

    /** Teléfono o email a donde va el mensaje, según el canal. */
    public String destino(Canal canal) {
        return canal == Canal.WHATSAPP ? telefono : email;
    }

    /** Nunca el email ni el teléfono: si el registro termina en un log por error, no filtra datos de contacto. */
    @Override
    public String toString() {
        return "ContactoPaciente[tieneEmail=" + tieneEmail() + ", tieneWhatsapp=" + tieneWhatsapp
                + ", consentimiento=" + consentimiento + "]";
    }

    private static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
