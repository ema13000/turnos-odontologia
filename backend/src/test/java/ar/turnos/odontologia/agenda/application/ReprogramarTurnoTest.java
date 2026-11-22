package ar.turnos.odontologia.agenda.application;

import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DNI_PACIENTE_UNO;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DRA_A;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.OTRO_PACIENTE;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.PACIENTE_UNO;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_1;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_2;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.a;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.relojALas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Clock;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Escenarios de reprogramación del spec {@code agenda-turnos} (C-06). */
class ReprogramarTurnoTest {

    private final AgendaEnMemoria agenda = new AgendaEnMemoria();

    @Test
    @DisplayName("Reprogramar sobre el propio horario")
    void reprogramarSobreElPropioHorario() {
        // GIVEN un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal lo reprograma a las 10:15 con la Dra. A en el Sillón 1
        ReprogramarTurno.Reprogramacion resultado = reprogramar(relojALas(8, 0), original, a(10, 15), DRA_A, SILLON_1);

        // THEN el turno original queda en estado CANCELADO
        assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.CANCELADO);
        // AND existe un turno nuevo de "Paciente Uno" de 10:15 a 10:45 en estado RESERVADO
        Turno nuevo = agenda.turnos.porId(resultado.nuevo().id());
        assertThat(nuevo.pacienteId()).isEqualTo(PACIENTE_UNO);
        assertThat(nuevo.intervalo().inicio()).isEqualTo(a(10, 15));
        assertThat(nuevo.intervalo().fin()).isEqualTo(a(10, 45));
        assertThat(nuevo.estado()).isEqualTo(EstadoTurno.RESERVADO);
        // AND el turno nuevo tiene un número de reserva distinto al original
        assertThat(nuevo.numeroReserva()).isNotEqualTo(original.numeroReserva());
    }

    @Test
    @DisplayName("Reprogramar a un horario en conflicto")
    void reprogramarAUnHorarioEnConflicto() {
        // GIVEN un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        // AND otro turno activo de 11:00 a 11:30 con la Dra. A en el Sillón 2
        agenda.existente(OTRO_PACIENTE, DRA_A, SILLON_2, a(11, 0), EstadoTurno.RESERVADO);

        // WHEN el personal reprograma el primero a las 11:15 con la Dra. A en el Sillón 1
        // THEN la operación se rechaza con SOLAPAMIENTO_PROFESIONAL
        assertRechazo(() -> reprogramar(relojALas(8, 0), original, a(11, 15), DRA_A, SILLON_1),
                CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
        // AND el turno original sigue en estado RESERVADO de 10:00 a 10:30
        Turno guardado = agenda.turnos.porId(original.id());
        assertThat(guardado.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(guardado.intervalo().inicio()).isEqualTo(a(10, 0));
        // AND no se crea ningún turno nuevo
        assertThat(agenda.turnos.todos()).hasSize(2);
    }

    @Test
    @DisplayName("Reprogramar al pasado")
    void reprogramarAlPasado() {
        // GIVEN que el momento actual es el 2026-12-01 a las 09:00
        // AND un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal lo reprograma a las 08:30
        // THEN la operación se rechaza con INICIO_EN_EL_PASADO
        assertRechazo(() -> reprogramar(relojALas(9, 0), original, a(8, 30), null, null),
                CodigoRechazo.INICIO_EN_EL_PASADO);
        // AND el turno original sigue en estado RESERVADO
        assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    @DisplayName("Reprogramar un turno que no está activo")
    void reprogramarUnTurnoQueNoEstaActivo() {
        // GIVEN un turno CANCELADO de "Paciente Uno" de 10:00 a 10:30
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.CANCELADO);

        // WHEN el personal lo reprograma a las 11:00 con el DNI y el número de reserva correctos
        // THEN la operación se rechaza con TURNO_NO_ACTIVO
        assertRechazo(() -> reprogramar(relojALas(8, 0), original, a(11, 0), null, null),
                CodigoRechazo.TURNO_NO_ACTIVO);
    }

    private ReprogramarTurno.Reprogramacion reprogramar(Clock reloj, Turno original, Instant inicio,
            Long profesional, Long sillon) {
        return agenda.reprogramarTurno(reloj).ejecutar(new ReprogramarTurnoCommand(original.id(),
                DNI_PACIENTE_UNO, original.numeroReserva().valor(), inicio, profesional, sillon));
    }

    private static void assertRechazo(ThrowingCallable accion, CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(esperado);
    }
}
