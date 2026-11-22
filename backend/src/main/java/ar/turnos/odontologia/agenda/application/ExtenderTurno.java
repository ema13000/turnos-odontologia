package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Caso de uso: extender el fin de un turno activo y no sobreturno (US-014, RN-AG-14). Reutiliza las
 * validaciones de recursos y conflictos de {@link CrearTurno} con el fin informado y el propio turno ignorado
 * como conflicto, y actualiza la misma fila sin cambiar id, número de reserva ni estado. El turno puede estar
 * ya iniciado (la extensión sirve justamente para eso); el nuevo fin tiene que estar en el futuro.
 */
public class ExtenderTurno {

    private final TurnoRepository turnos;
    private final CrearTurno crearTurno;

    public ExtenderTurno(TurnoRepository turnos, CrearTurno crearTurno) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
        this.crearTurno = Objects.requireNonNull(crearTurno, "crearTurno");
    }

    public Turno ejecutar(ExtenderTurnoCommand comando) {
        Turno original = turnos.buscarParaModificar(comando.turnoId())
                .orElseThrow(() -> new TurnoInexistenteException(comando.turnoId())).turno();
        validar(original, comando);

        CrearTurnoCommand datos = new CrearTurnoCommand(original.pacienteId(), original.profesionalId(),
                original.sillonId(), original.prestacionId(), original.intervalo().inicio());
        try {
            crearTurno.prepararExtension(datos, original.id(), comando.fin());
        } catch (TurnoRechazadoException e) {
            if (e.codigo() == CodigoRechazo.MARGEN_INSUFICIENTE && tocaElSillon(original, comando.fin())) {
                // La EXCLUDE de la base mete el margen dentro del rango: si el fin toca el turno siguiente,
                // la violación es del sillón, no un margen insuficiente (igual que en la creación normal).
                throw new TurnoRechazadoException(CodigoRechazo.SOLAPAMIENTO_SILLON,
                        "El sillón ya está ocupado en ese horario");
            }
            throw e;
        }

        return turnos.guardar(original.conExtension(comando.fin(), comando.motivo()));
    }

    /** Verdadero si otro turno del mismo sillón empieza exactamente en el fin o antes. */
    private boolean tocaElSillon(Turno original, Instant fin) {
        Instant desde = original.intervalo().inicio();
        return turnos.buscarQueIntersecan(null, original.sillonId(),
                // PostgreSQL guarda TIMESTAMPTZ con precisión de microsegundos: un nanosegundo se pierde
                // en el parámetro JDBC y el turno que empieza justo en el fin quedaría fuera de la ventana.
                new Intervalo(desde, fin.plus(1, ChronoUnit.MICROS))).stream()
                .anyMatch(t -> !original.id().equals(t.id()) && !t.intervalo().inicio().isAfter(fin));
    }

    private static void validar(Turno original, ExtenderTurnoCommand comando) {
        if (!original.esActivo()) {
            throw new TurnoRechazadoException(CodigoRechazo.TURNO_NO_ACTIVO, "El turno no está activo");
        }
        if (original.esSobreturno()) {
            throw new TurnoRechazadoException(CodigoRechazo.TURNO_NO_EXTENSIBLE,
                    "Un sobreturno no se puede extender");
        }
        if (!comando.fin().isAfter(original.intervalo().fin())) {
            throw new TurnoRechazadoException(CodigoRechazo.DATOS_INVALIDOS,
                    "El nuevo fin debe ser posterior al fin actual del turno");
        }
        if (comando.motivo().isBlank() || comando.motivo().trim().length() > 500) {
            throw new TurnoRechazadoException(CodigoRechazo.DATOS_INVALIDOS,
                    "El motivo administrativo de la extensión es obligatorio y no puede superar 500 caracteres");
        }
    }
}
