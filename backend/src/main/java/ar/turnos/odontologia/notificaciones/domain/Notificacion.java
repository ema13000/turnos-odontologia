package ar.turnos.odontologia.notificaciones.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Una notificación de un turno. Es inmutable: cada transición devuelve una copia.
 *
 * @param tokenConfirmacion huella SHA-256 del token de confirmación (nunca el token), o {@code null} si el tipo no
 *     lleva token
 * @param usadoEn cuándo se usó el token, o {@code null} si todavía no
 * @param intentos intentos fallidos en el canal actual
 * @param proximoIntento cuándo se reintenta, o {@code null} si no hay más reintentos
 * @param canalAlternativo {@code true} si la notificación pasó a email tras fallar por WhatsApp
 */
public record Notificacion(Long id, Long turnoId, Canal canal, TipoNotificacion tipo, EstadoNotificacion estado,
                           Instant programadaPara, Instant enviadaEn, String tokenConfirmacion, Instant usadoEn,
                           int intentos, Instant proximoIntento, boolean canalAlternativo) {

    /** Una notificación PENDIENTE que no se envió en este tiempo quedó interrumpida y se reprocesa. */
    public static final java.time.Duration ESPERA_PENDIENTE = java.time.Duration.ofMinutes(10);

    public Notificacion {
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(programadaPara, "programadaPara");
    }

    /** Notificación nueva, todavía sin guardar, lista para enviarse. */
    public static Notificacion pendiente(Long turnoId, Canal canal, TipoNotificacion tipo, Instant programadaPara,
                                         String huellaToken) {
        return new Notificacion(null, turnoId, canal, tipo, EstadoNotificacion.PENDIENTE, programadaPara, null,
                huellaToken, null, 0, null, false);
    }

    public Notificacion conId(Long nuevoId) {
        return new Notificacion(Objects.requireNonNull(nuevoId, "nuevoId"), turnoId, canal, tipo, estado,
                programadaPara, enviadaEn, tokenConfirmacion, usadoEn, intentos, proximoIntento, canalAlternativo);
    }

    /** Otra huella de token: en cada intento se genera un token nuevo, porque el anterior no se guarda en claro. */
    public Notificacion conHuellaDeToken(String huella) {
        return new Notificacion(id, turnoId, canal, tipo, estado, programadaPara, enviadaEn, huella, usadoEn,
                intentos, proximoIntento, canalAlternativo);
    }

    public Notificacion enviada(Instant ahora) {
        return new Notificacion(id, turnoId, canal, tipo, EstadoNotificacion.ENVIADA, programadaPara, ahora,
                tokenConfirmacion, usadoEn, intentos, null, canalAlternativo);
    }

    /** Falló un intento: queda {@code FALLIDA} y, si la política lo permite, con el próximo intento programado. */
    public Notificacion fallida(Instant ahora, boolean tieneEmail) {
        int fallas = intentos + 1;
        Instant proximo = PoliticaReintentos.haySiguienteIntento(canal, fallas, tieneEmail)
                ? ahora.plus(PoliticaReintentos.espera(Math.min(fallas, PoliticaReintentos.MAX_INTENTOS)))
                : null;
        return new Notificacion(id, turnoId, canal, tipo, EstadoNotificacion.FALLIDA, programadaPara, enviadaEn,
                tokenConfirmacion, usadoEn, fallas, proximo, canalAlternativo);
    }

    /** Sin más reintentos, por ejemplo porque el turno se canceló o el paciente retiró el consentimiento. */
    public Notificacion abandonada() {
        return new Notificacion(id, turnoId, canal, tipo, EstadoNotificacion.FALLIDA, programadaPara, enviadaEn,
                tokenConfirmacion, usadoEn, intentos, null, canalAlternativo);
    }

    public boolean debeCambiarAEmail(boolean tieneEmail) {
        return PoliticaReintentos.cambiaAEmail(canal, intentos, tieneEmail);
    }

    /** La misma notificación por email, con los intentos en cero (design D6: la misma fila, no una nueva). */
    public Notificacion aEmail() {
        return new Notificacion(id, turnoId, Canal.EMAIL, tipo, EstadoNotificacion.PENDIENTE, programadaPara,
                enviadaEn, tokenConfirmacion, usadoEn, 0, null, true);
    }

    /**
     * Hay que reprocesarla: falló y le llegó la hora del reintento, o quedó PENDIENTE sin enviarse más de
     * {@link #ESPERA_PENDIENTE} (un corte entre el registro y el envío).
     */
    public boolean reintentoVencido(Instant ahora) {
        boolean fallidaVencida = estado == EstadoNotificacion.FALLIDA && proximoIntento != null
                && !ahora.isBefore(proximoIntento);
        boolean pendienteInterrumpida = estado == EstadoNotificacion.PENDIENTE
                && !programadaPara.isAfter(ahora.minus(ESPERA_PENDIENTE));
        return fallidaVencida || pendienteInterrumpida;
    }

    /**
     * La fila tomada por una corrida: queda PENDIENTE con {@code programadaPara = ahora}, que actúa de reserva por
     * {@link #ESPERA_PENDIENTE}: otra corrida no la toma y, si esta se corta, se recupera después.
     */
    public Notificacion reclamada(Instant ahora) {
        return new Notificacion(id, turnoId, canal, tipo, EstadoNotificacion.PENDIENTE, ahora, enviadaEn,
                tokenConfirmacion, usadoEn, intentos, null, canalAlternativo);
    }
}
