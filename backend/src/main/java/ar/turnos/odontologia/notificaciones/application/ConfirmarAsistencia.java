package ar.turnos.odontologia.notificaciones.application;

import ar.turnos.odontologia.notificaciones.domain.CodigoConfirmacion;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import ar.turnos.odontologia.notificaciones.domain.Notificacion;
import java.time.Clock;
import java.util.Objects;

/**
 * Caso de uso (US-031): el paciente confirma su asistencia con el token del recordatorio, sin sesión. El token se
 * busca por su huella SHA-256, nunca en claro, y se consume una sola vez (design D4).
 *
 * <p>Se confirma el turno antes de consumir el token: si el turno ya no se puede confirmar el token no se gasta.
 * Quien lo llama (el servicio transaccional) hace que las dos cosas y la auditoría ocurran juntas o ninguna.
 */
public class ConfirmarAsistencia {

    private final NotificacionRepository notificaciones;
    private final ConfirmadorDeTurnos confirmador;
    private final Clock reloj;

    public ConfirmarAsistencia(NotificacionRepository notificaciones, ConfirmadorDeTurnos confirmador, Clock reloj) {
        this.notificaciones = Objects.requireNonNull(notificaciones, "notificaciones");
        this.confirmador = Objects.requireNonNull(confirmador, "confirmador");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /** @throws ConfirmacionRechazadaException {@code TOKEN_INEXISTENTE}, {@code TOKEN_USADO} o {@code TURNO_NO_CONFIRMABLE} */
    public TurnoConfirmado ejecutar(String token) {
        String huella = HuellaToken.de(token == null ? "" : token);
        Notificacion notificacion = notificaciones.buscarPorHuella(huella)
                .orElseThrow(() -> new ConfirmacionRechazadaException(CodigoConfirmacion.TOKEN_INEXISTENTE));
        if (notificacion.usadoEn() != null) {
            throw new ConfirmacionRechazadaException(CodigoConfirmacion.TOKEN_USADO);
        }
        TurnoConfirmado confirmado;
        try {
            confirmado = confirmador.confirmar(notificacion.turnoId());
        } catch (ConfirmacionRechazadaException e) {
            // si otra confirmación con el mismo token ganó mientras tanto, el turno ya no está RESERVADO: es un
            // segundo uso del token, no un turno que no se puede confirmar
            if (estaUsado(huella)) {
                throw new ConfirmacionRechazadaException(CodigoConfirmacion.TOKEN_USADO);
            }
            throw e;
        }
        if (!notificaciones.consumirToken(huella, reloj.instant())) {
            // perdió la carrera por el token: quien llama deshace la transacción y el turno no queda confirmado dos veces
            throw new ConfirmacionRechazadaException(CodigoConfirmacion.TOKEN_USADO);
        }
        return confirmado;
    }

    private boolean estaUsado(String huella) {
        return notificaciones.buscarPorHuella(huella).map(n -> n.usadoEn() != null).orElse(false);
    }
}
