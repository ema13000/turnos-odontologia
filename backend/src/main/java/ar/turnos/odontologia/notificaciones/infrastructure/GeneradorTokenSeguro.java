package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.notificaciones.application.GeneradorToken;
import java.security.SecureRandom;
import java.util.Base64;

/** Token de 256 bits (32 bytes de {@link SecureRandom}) en base64url sin relleno: seguro para ir en un link. */
class GeneradorTokenSeguro implements GeneradorToken {

    static final int BYTES = 32;

    private final SecureRandom azar;

    GeneradorTokenSeguro(SecureRandom azar) {
        this.azar = azar;
    }

    @Override
    public String generar() {
        byte[] bytes = new byte[BYTES];
        azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
