package ar.turnos.odontologia.agenda.infrastructure;

import ar.turnos.odontologia.agenda.application.AuditoriaTurnos;
import ar.turnos.odontologia.agenda.application.AuditoriaTurnos.Accion;
import ar.turnos.odontologia.agenda.application.CancelarTurno;
import ar.turnos.odontologia.agenda.application.CancelarTurnoCommand;
import ar.turnos.odontologia.agenda.application.ConfirmarTurno;
import ar.turnos.odontologia.agenda.application.ConsultaAgenda;
import ar.turnos.odontologia.agenda.application.CrearTurno;
import ar.turnos.odontologia.agenda.application.CrearTurnoCommand;
import ar.turnos.odontologia.agenda.application.ReprogramarTurno;
import ar.turnos.odontologia.agenda.application.ReprogramarTurnoCommand;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Borde transaccional de la agenda (design D5): cada operación y su registro de auditoría ocurren juntos o no
 * ocurre ninguno. Si un caso de uso o la base rechazan la operación, se deshace todo (RN-AG-08, RN-AG-12).
 *
 * <p>Dos inserciones simultáneas que se pisan pueden quedar esperándose entre sí al verificar el {@code EXCLUDE},
 * y PostgreSQL aborta una por deadlock. Esa transacción se reintenta entera: en el reintento el otro turno ya
 * está confirmado y el dominio responde el rechazo correcto, nunca un error 500 (design D6).
 */
@Service
public class ServicioAgenda {

    static final int INTENTOS = 3;

    private final CrearTurno crearTurno;
    private final CancelarTurno cancelarTurno;
    private final ReprogramarTurno reprogramarTurno;
    private final ConfirmarTurno confirmarTurno;
    private final ConsultaAgenda consulta;
    private final AuditoriaTurnos auditoria;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate lectura;

    ServicioAgenda(CrearTurno crearTurno, CancelarTurno cancelarTurno, ReprogramarTurno reprogramarTurno,
                   ConfirmarTurno confirmarTurno, ConsultaAgenda consulta, AuditoriaTurnos auditoria,
                   PlatformTransactionManager transacciones) {
        this.crearTurno = crearTurno;
        this.cancelarTurno = cancelarTurno;
        this.reprogramarTurno = reprogramarTurno;
        this.confirmarTurno = confirmarTurno;
        this.consulta = consulta;
        this.auditoria = auditoria;
        this.transaccion = new TransactionTemplate(transacciones);
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
    }

    public Turno crear(CrearTurnoCommand comando, String actor) {
        return enTransaccion(() -> {
            Turno turno = crearTurno.ejecutar(comando);
            auditoria.registrar(turno.id(), Accion.CREAR, actor, null);
            return turno;
        });
    }

    public Turno cancelar(CancelarTurnoCommand comando, String actor) {
        return enTransaccion(() -> {
            Turno turno = cancelarTurno.ejecutar(comando);
            auditoria.registrar(turno.id(), Accion.CANCELAR, actor, null);
            return turno;
        });
    }

    public Turno reprogramar(ReprogramarTurnoCommand comando, String actor) {
        return enTransaccion(() -> {
            ReprogramarTurno.Reprogramacion resultado = reprogramarTurno.ejecutar(comando);
            auditoria.registrar(resultado.original().id(), Accion.REPROGRAMAR, actor, resultado.nuevo().id());
            auditoria.registrar(resultado.nuevo().id(), Accion.CREAR, actor, null);
            return resultado.nuevo();
        });
    }

    /**
     * Confirma un turno {@code RESERVADO} (C-11) y registra la auditoría {@code CONFIRMAR} en la misma transacción.
     * Si el turno no está reservado el caso de uso lo rechaza y no queda ningún registro. Si ya hay una
     * transacción abierta (la confirmación por token), participa de ella.
     */
    public Turno confirmar(Long turnoId, String actor) {
        return enTransaccion(() -> {
            Turno turno = confirmarTurno.ejecutar(turnoId);
            auditoria.registrar(turno.id(), Accion.CONFIRMAR, actor, null);
            return turno;
        });
    }

    public List<ConsultaAgenda.TurnoEnAgenda> agenda(Instant desde, Instant hasta, Long profesionalId, Long sillonId) {
        return lectura.execute(estado -> consulta.turnosActivos(desde, hasta, profesionalId, sillonId));
    }

    /** Ejecuta la operación en una transacción y la reintenta entera si la base la abortó por un bloqueo. */
    private Turno enTransaccion(Supplier<Turno> operacion) {
        for (int intento = 1; ; intento++) {
            try {
                return transaccion.execute(estado -> operacion.get());
            } catch (PessimisticLockingFailureException e) {
                if (intento >= INTENTOS) {
                    throw e;
                }
            }
        }
    }
}
