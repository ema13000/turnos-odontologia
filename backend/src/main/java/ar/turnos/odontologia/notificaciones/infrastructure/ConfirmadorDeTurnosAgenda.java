package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import ar.turnos.odontologia.agenda.infrastructure.ServicioAgenda;
import ar.turnos.odontologia.notificaciones.application.ConfirmadorDeTurnos;
import ar.turnos.odontologia.notificaciones.application.TurnoConfirmado;
import ar.turnos.odontologia.notificaciones.application.TurnosARecordar;
import ar.turnos.odontologia.notificaciones.domain.CodigoConfirmacion;
import ar.turnos.odontologia.notificaciones.domain.ConfirmacionRechazadaException;
import ar.turnos.odontologia.notificaciones.domain.TurnoARecordar;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto hacia la agenda (design D3): confirma el turno con {@link ServicioAgenda}, que registra la
 * auditoría {@code CONFIRMAR} con el actor {@code paciente-token}. Participa de la transacción de quien lo llama.
 * El token no llega hasta acá: la agenda y su auditoría nunca lo ven.
 */
@Component
class ConfirmadorDeTurnosAgenda implements ConfirmadorDeTurnos {

    static final String ACTOR = "paciente-token";

    private final ServicioAgenda agenda;
    private final TurnosARecordar turnos;
    private final JdbcTemplate jdbc;

    ConfirmadorDeTurnosAgenda(ServicioAgenda agenda, TurnosARecordar turnos, JdbcTemplate jdbc) {
        this.agenda = agenda;
        this.turnos = turnos;
        this.jdbc = jdbc;
    }

    @Override
    public TurnoConfirmado confirmar(Long turnoId) {
        try {
            agenda.confirmar(turnoId, ACTOR);
        } catch (TurnoRechazadoException | TurnoInexistenteException e) {
            throw new ConfirmacionRechazadaException(CodigoConfirmacion.TURNO_NO_CONFIRMABLE);
        }
        TurnoARecordar turno = turnos.deTurno(turnoId).orElseThrow();
        // el nombre del profesional va solo en la respuesta al paciente que tiene el token, nunca en un mensaje
        String profesional = jdbc.queryForObject("SELECT pr.nombre || ' ' || pr.apellido FROM turno t"
                + " JOIN profesional pr ON pr.id = t.profesional_id WHERE t.id = ?", String.class, turnoId);
        return new TurnoConfirmado(turno.inicio(), profesional);
    }
}
