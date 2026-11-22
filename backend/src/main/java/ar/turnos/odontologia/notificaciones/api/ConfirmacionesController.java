package ar.turnos.odontologia.notificaciones.api;

import ar.turnos.odontologia.notificaciones.application.TurnoConfirmado;
import ar.turnos.odontologia.notificaciones.domain.CodigoConfirmacion;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import ar.turnos.odontologia.notificaciones.domain.MensajeRecordatorio;
import ar.turnos.odontologia.notificaciones.infrastructure.ServicioNotificaciones;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirmación de asistencia con el token del recordatorio (US-031). El endpoint es público
 * ({@code /api/v1/public/**}) pero sigue exigiendo CSRF, y es solo {@code POST}: un {@code GET} no consume el
 * token, porque los escáneres de links de WhatsApp y de email abren los links sin que el paciente haga nada. El link
 * del mensaje apunta a una pantalla del frontend que hace este POST.
 */
@RestController
@RequestMapping("/api/v1/public/confirmaciones")
class ConfirmacionesController {

    /** Lo que genera el servidor: base64url de 256 bits. Cualquier otra cosa nunca se emitió. */
    private static final Pattern FORMATO_DE_TOKEN = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(MensajeRecordatorio.ZONA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(MensajeRecordatorio.ZONA);

    private final ServicioNotificaciones servicio;

    ConfirmacionesController(ServicioNotificaciones servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/{token}")
    ConfirmacionResponse confirmar(@PathVariable String token) {
        if (!FORMATO_DE_TOKEN.matcher(token).matches()) {
            throw new ConfirmacionRechazadaException(CodigoConfirmacion.TOKEN_INEXISTENTE);
        }
        TurnoConfirmado confirmado = servicio.confirmar(token);
        return new ConfirmacionResponse(FECHA.format(confirmado.inicio()), HORA.format(confirmado.inicio()),
                confirmado.nombreProfesional(), "CONFIRMADO");
    }
}
