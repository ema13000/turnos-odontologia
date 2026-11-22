package ar.turnos.odontologia.notificaciones.domain;

import java.time.Duration;
import java.util.List;

/**
 * Política de reintentos (design D6): hasta 3 intentos por canal, con espera creciente de 5, 10 y 20 minutos. Tras
 * la tercera falla por WhatsApp, si el paciente tiene email se prueba por email; el email nunca vuelve a WhatsApp.
 * Agotado eso, la notificación queda {@code FALLIDA} sin más reintentos.
 */
public final class PoliticaReintentos {

    public static final int MAX_INTENTOS = 3;

    private static final List<Duration> ESPERAS = List.of(
            Duration.ofMinutes(5), Duration.ofMinutes(10), Duration.ofMinutes(20));

    private PoliticaReintentos() {}

    /** Espera después de la falla número {@code intentosFallidos} (de 1 a 3). */
    public static Duration espera(int intentosFallidos) {
        if (intentosFallidos < 1 || intentosFallidos > MAX_INTENTOS) {
            throw new IllegalArgumentException("Las fallas con espera son de 1 a " + MAX_INTENTOS);
        }
        return ESPERAS.get(intentosFallidos - 1);
    }

    /** Pasa de WhatsApp a email: agotó los intentos por WhatsApp y el paciente tiene email. */
    public static boolean cambiaAEmail(Canal canal, int intentosFallidos, boolean tieneEmail) {
        return canal == Canal.WHATSAPP && intentosFallidos >= MAX_INTENTOS && tieneEmail;
    }

    /** Si queda algo por intentar: otro intento en el mismo canal o el respaldo por email. */
    public static boolean haySiguienteIntento(Canal canal, int intentosFallidos, boolean tieneEmail) {
        return intentosFallidos < MAX_INTENTOS || cambiaAEmail(canal, intentosFallidos, tieneEmail);
    }
}
