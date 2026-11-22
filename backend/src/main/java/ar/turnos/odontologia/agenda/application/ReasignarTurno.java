package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import ar.turnos.odontologia.configuracion.application.HabilitacionProfesional;
import java.util.Objects;

/**
 * Caso de uso: el consultorio reasigna un turno activo a otro profesional o a otro horario (RN-AG-16, C-07 design
 * D10). Es una reprogramación sin la identificación del paciente de RN-AG-07: cancela el original y crea uno nuevo
 * con todas las reglas de {@link CrearTurno} (pasado, recursos, ausencias, bloqueos, horario y solapamientos) y un
 * número de reserva nuevo. Un rechazo deja el original intacto.
 */
public class ReasignarTurno {

    private final TurnoRepository turnos;
    private final CrearTurno crearTurno;
    private final HabilitacionProfesional habilitacion;

    public ReasignarTurno(TurnoRepository turnos, CrearTurno crearTurno, HabilitacionProfesional habilitacion) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
        this.crearTurno = Objects.requireNonNull(crearTurno, "crearTurno");
        this.habilitacion = Objects.requireNonNull(habilitacion, "habilitacion");
    }

    public ReprogramarTurno.Reprogramacion ejecutar(ReasignarTurnoCommand comando) {
        Turno original = turnos.buscarParaModificar(comando.turnoId())
                .orElseThrow(() -> new TurnoInexistenteException(comando.turnoId())).turno();
        if (!original.esActivo()) {
            throw new TurnoRechazadoException(CodigoRechazo.TURNO_NO_ACTIVO, "El turno no está activo");
        }

        Long profesional = comando.profesionalId() != null ? comando.profesionalId() : original.profesionalId();
        // RN-AG-15 se exige al pasar el turno a otro profesional: con el mismo profesional, el turno ya estaba dado.
        if (!profesional.equals(original.profesionalId())
                && !habilitacion.estaHabilitado(profesional, original.prestacionId())) {
            throw new TurnoRechazadoException(CodigoRechazo.PROFESIONAL_NO_HABILITADO,
                    "El profesional no está habilitado para la prestación del turno");
        }

        CrearTurnoCommand datosNuevos = new CrearTurnoCommand(
                original.pacienteId(),
                profesional,
                comando.sillonId() != null ? comando.sillonId() : original.sillonId(),
                original.prestacionId(),
                comando.inicio() != null ? comando.inicio() : original.intervalo().inicio());
        Turno nuevo = crearTurno.preparar(datosNuevos, original.id());

        Turno cancelado = turnos.guardar(original.cancelado());
        return new ReprogramarTurno.Reprogramacion(cancelado, turnos.guardar(nuevo));
    }
}
