package ar.turnos.odontologia.agenda.disponibilidad.api;

import ar.turnos.odontologia.agenda.disponibilidad.application.RecursoNoEncontradoException;
import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import ar.turnos.odontologia.agenda.domain.reglas.CodigoDisponibilidad;
import ar.turnos.odontologia.agenda.domain.reglas.DisponibilidadException;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Errores de la API de disponibilidad (design D8), en {@code ProblemDetail} con la propiedad {@code codigo}:
 * datos inválidos → 422, recurso inexistente → 404 y rechazos de la agenda → 409. Los 403 los escribe la
 * seguridad: este manejador no atrapa {@code AccessDeniedException}.
 */
@RestControllerAdvice(basePackages = "ar.turnos.odontologia.agenda.disponibilidad.api")
class DisponibilidadExceptionHandler {

    private static final String DATOS_INVALIDOS = "DATOS_INVALIDOS";

    /** Restricciones de V5 que el dominio ya valida antes (doble garantía, design D7). */
    private static final Map<String, CodigoDisponibilidad> RESTRICCIONES = Map.of(
            "horario_trabajo_sin_superposicion", CodigoDisponibilidad.FRANJAS_SUPERPUESTAS,
            "horario_trabajo_rango_valido", CodigoDisponibilidad.HORARIO_INVALIDO,
            "horario_trabajo_dia_valido", CodigoDisponibilidad.HORARIO_INVALIDO,
            "ausencia_profesional_periodo_valido", CodigoDisponibilidad.PERIODO_INVALIDO,
            "bloqueo_agenda_periodo_valido", CodigoDisponibilidad.PERIODO_INVALIDO);

    @ExceptionHandler(DisponibilidadException.class)
    ProblemDetail datoInvalido(DisponibilidadException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, e.codigo().name(), e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(TurnoInexistenteException.class)
    ProblemDetail turnoInexistente(TurnoInexistenteException e) {
        return problema(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "No existe el turno");
    }

    @ExceptionHandler(TurnoRechazadoException.class)
    ProblemDetail turnoRechazado(TurnoRechazadoException e) {
        HttpStatus status = e.codigo() == CodigoRechazo.DATOS_INVALIDOS
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.CONFLICT;
        return problema(status, e.codigo().name(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        ProblemDetail problema = problema(HttpStatus.UNPROCESSABLE_ENTITY, DATOS_INVALIDOS,
                "Hay datos de entrada inválidos");
        List<Map<String, String>> errores = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("campo", error.getField(), "mensaje", String.valueOf(error.getDefaultMessage())))
                .toList();
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail ilegible(Exception e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, DATOS_INVALIDOS, "El pedido no es válido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        String restriccion = e.getCause() instanceof ConstraintViolationException cve ? cve.getConstraintName() : null;
        CodigoDisponibilidad codigo = restriccion == null ? null : RESTRICCIONES.get(restriccion);
        if (codigo == null) {
            throw e;
        }
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, codigo.name(), "Los datos violan la restricción " + restriccion);
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(codigo);
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
