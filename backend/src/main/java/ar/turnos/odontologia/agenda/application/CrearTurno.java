package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Agenda;
import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.ReferenciaInexistenteException;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: crear un turno sin solapamientos por profesional ni por sillón (US-010).
 *
 * <p>Orden de validación: primero el pasado (RN-AG-09), después que existan los recursos y que estén
 * activos (RN-AG-11), el cálculo del fin (RN-AG-01) y por último los conflictos con margen
 * (RN-AG-02 a RN-AG-05).
 */
public class CrearTurno {

    private final TurnoRepository turnos;
    private final RecursosAgenda recursos;
    private final GeneradorNumeroReserva numeros;
    private final Clock reloj;
    private final Margen margen;
    private final Agenda agenda;

    public CrearTurno(TurnoRepository turnos, RecursosAgenda recursos, GeneradorNumeroReserva numeros,
            Clock reloj, Margen margen) {
        this.turnos = Objects.requireNonNull(turnos, "turnos");
        this.recursos = Objects.requireNonNull(recursos, "recursos");
        this.numeros = Objects.requireNonNull(numeros, "numeros");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.margen = Objects.requireNonNull(margen, "margen");
        this.agenda = new Agenda(margen);
    }

    public Turno ejecutar(CrearTurnoCommand comando) {
        return turnos.guardar(preparar(comando, null));
    }

    /** Firma original: el fin se calcula con la duración de la prestación. */
    Turno preparar(CrearTurnoCommand comando, Long reemplazaA) {
        return preparar(comando, reemplazaA, null);
    }

    /**
     * Arma y verifica un turno nuevo sin guardarlo. Si {@code reemplazaA} no es nulo, ese turno no cuenta
     * como conflicto: es el original de una reprogramación, que se cancela en la misma operación.
     * {@code finAlternativo} permite reutilizar todas las validaciones en la extensión de un turno.
     */
    Turno preparar(CrearTurnoCommand comando, Long reemplazaA, Instant finAlternativo) {
        if (comando.inicio().isBefore(reloj.instant())) {
            throw new TurnoRechazadoException(CodigoRechazo.INICIO_EN_EL_PASADO,
                    "No se puede crear un turno en el pasado");
        }
        return armar(comando, reemplazaA, finAlternativo);
    }

    /**
     * Verifica la extensión del turno {@code turnoId}, que puede estar ya iniciado: lo que no puede quedar en el
     * pasado es el nuevo fin, no el inicio. Es el único camino que omite el control de inicio de
     * {@link #preparar}: crear y reprogramar siguen rechazando inicios pasados (RN-AG-09).
     */
    Turno prepararExtension(CrearTurnoCommand comando, Long turnoId, Instant nuevoFin) {
        if (!nuevoFin.isAfter(reloj.instant())) {
            throw new TurnoRechazadoException(CodigoRechazo.DATOS_INVALIDOS,
                    "El nuevo fin debe ser posterior al momento actual");
        }
        return armar(comando, turnoId, nuevoFin);
    }

    private Turno armar(CrearTurnoCommand comando, Long reemplazaA, Instant finAlternativo) {
        RecursosAgenda.Prestacion prestacion = verificarRecursos(comando);
        Instant fin = finAlternativo != null ? finAlternativo : comando.inicio().plus(prestacion.duracion());
        Intervalo intervalo = new Intervalo(comando.inicio(), fin);

        List<Turno> cercanos = turnos.buscarQueIntersecan(comando.profesionalId(),
                comando.sillonId(), intervalo.ampliadoPor(margen)).stream()
                .filter(t -> reemplazaA == null || !reemplazaA.equals(t.id()))
                .toList();
        Turno nuevo = Turno.reservar(comando.pacienteId(), comando.profesionalId(),
                comando.sillonId(), comando.prestacionId(), intervalo, numeros.generar());
        agenda.verificar(nuevo, cercanos);
        return nuevo;
    }

    private RecursosAgenda.Prestacion verificarRecursos(CrearTurnoCommand comando) {
        if (!recursos.existePaciente(comando.pacienteId())) {
            throw new ReferenciaInexistenteException("pacienteId");
        }
        boolean profesionalActivo = existente(recursos.profesionalActivo(comando.profesionalId()), "profesionalId");
        boolean sillonActivo = existente(recursos.sillonActivo(comando.sillonId()), "sillonId");
        RecursosAgenda.Prestacion prestacion = existente(recursos.prestacion(comando.prestacionId()), "prestacionId");

        if (!profesionalActivo) {
            throw inactivo("El profesional está inactivo");
        }
        if (!sillonActivo) {
            throw inactivo("El sillón está inactivo");
        }
        if (!prestacion.activa()) {
            throw inactivo("La prestación está inactiva");
        }
        return prestacion;
    }

    private static <T> T existente(Optional<T> valor, String campo) {
        return valor.orElseThrow(() -> new ReferenciaInexistenteException(campo));
    }

    private static TurnoRechazadoException inactivo(String mensaje) {
        return new TurnoRechazadoException(CodigoRechazo.RECURSO_INACTIVO, mensaje);
    }
}
