package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Agenda;
import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: crear un turno sin solapamientos por profesional ni por sillón (US-010).
 *
 * <p>Orden de validación: primero el pasado (RN-AG-09), después el cálculo del fin (RN-AG-01)
 * y por último los conflictos con margen (RN-AG-02 a RN-AG-05).
 */
public class CrearTurno {

    private final TurnoRepository turnos;
    private final PrestacionRepository prestaciones;
    private final Clock reloj;
    private final Margen margen;
    private final Agenda agenda;

    public CrearTurno(TurnoRepository turnos, PrestacionRepository prestaciones, Clock reloj, Margen margen) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
        this.prestaciones = Objects.requireNonNull(prestaciones, "prestaciones");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.margen = Objects.requireNonNull(margen, "margen");
        this.agenda = new Agenda(margen);
    }

    public Turno ejecutar(CrearTurnoCommand comando) {
        Instant ahora = reloj.instant();
        if (comando.inicio().isBefore(ahora)) {
            throw new TurnoRechazadoException(CodigoRechazo.INICIO_EN_EL_PASADO,
                    "No se puede crear un turno en el pasado");
        }

        Duration duracion = prestaciones.duracionDe(comando.prestacionId());
        Intervalo intervalo = new Intervalo(comando.inicio(), comando.inicio().plus(duracion));
        Turno nuevo = Turno.reservar(comando.pacienteId(), comando.profesionalId(),
                comando.sillonId(), comando.prestacionId(), intervalo);

        List<Turno> cercanos = turnos.buscarQueIntersecan(comando.profesionalId(),
                comando.sillonId(), intervalo.ampliadoPor(margen));
        agenda.verificar(nuevo, cercanos);

        turnos.guardar(nuevo);
        return nuevo;
    }
}
