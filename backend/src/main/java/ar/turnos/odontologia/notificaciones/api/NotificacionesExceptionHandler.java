package ar.turnos.odontologia.notificaciones.api;

import ar.turnos.odontologia.notificaciones.domain.CodigoConfirmacion;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores de los controllers de notificaciones, con el mismo formato que el resto de la API: {@code ProblemDetail}
 * con la propiedad {@code codigo}. Token inexistente → 404; token usado o turno no confirmable → 409. El detalle
 * nunca incluye el token.
 */
@RestControllerAdvice(basePackages = "ar.turnos.odontologia.notificaciones.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
class NotificacionesExceptionHandler {

    @ExceptionHandler(ConfirmacionRechazadaException.class)
    ProblemDetail confirmacionRechazada(ConfirmacionRechazadaException e) {
        CodigoConfirmacion codigo = e.codigo();
        return switch (codigo) {
            case TOKEN_INEXISTENTE -> problema(HttpStatus.NOT_FOUND, "Token inexistente", codigo,
                    "El link de confirmación no es válido");
            case TOKEN_USADO -> problema(HttpStatus.CONFLICT, "Token ya usado", codigo,
                    "El link de confirmación ya se usó");
            case TURNO_NO_CONFIRMABLE -> problema(HttpStatus.CONFLICT, "Turno no confirmable", codigo,
                    "El turno ya no se puede confirmar");
        };
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, CodigoConfirmacion codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo.name());
        return problema;
    }
}
