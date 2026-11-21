package ar.turnos.odontologia.agenda.application;

import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.CONSULTA;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DNI_PACIENTE_UNO;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DRA_A;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.OTRO_PACIENTE;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_1;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.a;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.relojALas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Escenarios de cancelación y de validación de DNI y número de reserva del spec {@code agenda-turnos} (C-06). */
class CancelarTurnoTest {

    private final AgendaEnMemoria agenda = new AgendaEnMemoria();

    // ---------- Requirement: Validación de DNI y número de reserva ----------

    @Test
    @DisplayName("DNI que no corresponde al turno")
    void dniQueNoCorresponde() {
        // GIVEN un turno activo de "Paciente Uno" (DNI 99000001) de 10:00 a 10:30
        Turno turno = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal lo cancela informando el DNI 99000002 y el número de reserva correcto
        // THEN la operación se rechaza con DATOS_DE_RESERVA_INVALIDOS
        assertRechazo(() -> cancelar(turno, "99000002", turno.numeroReserva().valor()),
                CodigoRechazo.DATOS_DE_RESERVA_INVALIDOS);
        // AND el turno sigue en estado RESERVADO
        assertThat(agenda.turnos.porId(turno.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    @DisplayName("Número de reserva que no corresponde al turno")
    void numeroQueNoCorresponde() {
        // GIVEN un turno activo de "Paciente Uno" (DNI 99000001) de 10:00 a 10:30
        Turno turno = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        Turno otro = agenda.existente(OTRO_PACIENTE, 2L, 2L, a(12, 0), EstadoTurno.RESERVADO);

        // WHEN el personal lo cancela informando el DNI 99000001 y un número de reserva de otro turno
        // THEN la operación se rechaza con DATOS_DE_RESERVA_INVALIDOS
        assertRechazo(() -> cancelar(turno, DNI_PACIENTE_UNO, otro.numeroReserva().valor()),
                CodigoRechazo.DATOS_DE_RESERVA_INVALIDOS);
        // AND el turno sigue en estado RESERVADO
        assertThat(agenda.turnos.porId(turno.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    // ---------- Requirement: Cancelar un turno ----------

    @Test
    @DisplayName("Cancelar libera el horario")
    void cancelarLiberaElHorario() {
        // GIVEN un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
        Turno turno = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal lo cancela con el DNI y el número de reserva correctos
        Turno cancelado = cancelar(turno, DNI_PACIENTE_UNO, turno.numeroReserva().valor());

        // THEN el turno queda en estado CANCELADO
        assertThat(cancelado.estado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(agenda.turnos.porId(turno.id()).estado()).isEqualTo(EstadoTurno.CANCELADO);
        // AND se puede crear otro turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
        Turno nuevo = agenda.crearTurno(relojALas(8, 0))
                .ejecutar(new CrearTurnoCommand(OTRO_PACIENTE, DRA_A, SILLON_1, CONSULTA, a(10, 0)));
        assertThat(nuevo.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    @DisplayName("Cancelar un turno ya cancelado")
    void cancelarUnTurnoYaCancelado() {
        // GIVEN un turno CANCELADO de "Paciente Uno" de 10:00 a 10:30
        Turno turno = agenda.dePacienteUno(a(10, 0), EstadoTurno.CANCELADO);

        // WHEN el personal lo cancela con el DNI y el número de reserva correctos
        // THEN la operación se rechaza con TURNO_NO_ACTIVO
        assertRechazo(() -> cancelar(turno, DNI_PACIENTE_UNO, turno.numeroReserva().valor()),
                CodigoRechazo.TURNO_NO_ACTIVO);
    }

    private Turno cancelar(Turno turno, String dni, String numeroReserva) {
        return agenda.cancelarTurno().ejecutar(new CancelarTurnoCommand(turno.id(), dni, numeroReserva));
    }

    private static void assertRechazo(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion,
            CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(esperado);
    }
}
