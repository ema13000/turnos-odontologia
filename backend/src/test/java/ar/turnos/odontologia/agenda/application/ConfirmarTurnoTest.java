package ar.turnos.odontologia.agenda.application;

import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.CONSULTA;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DRA_A;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_1;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.a;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.relojALas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Escenarios de "Confirmación de un turno reservado" del spec {@code agenda-turnos} (C-11). */
class ConfirmarTurnoTest {

    private final AgendaEnMemoria agenda = new AgendaEnMemoria();
    private final ConfirmarTurno confirmarTurno = new ConfirmarTurno(agenda.turnos);

    @Test
    @DisplayName("Confirmar un turno reservado")
    void confirmarUnTurnoReservado() {
        // GIVEN un turno RESERVADO de "Paciente Uno" de 10:00 a 10:30 con la Dra. A en el Sillón 1
        Turno reservado = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN se lo confirma
        Turno confirmado = confirmarTurno.ejecutar(reservado.id());

        // THEN el turno queda CONFIRMADO con el mismo horario, sillón, profesional y número de reserva
        Turno guardado = agenda.turnos.porId(reservado.id());
        assertThat(guardado.estado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(confirmado).isEqualTo(guardado);
        assertThat(guardado.intervalo()).isEqualTo(reservado.intervalo());
        assertThat(guardado.sillonId()).isEqualTo(SILLON_1);
        assertThat(guardado.profesionalId()).isEqualTo(DRA_A);
        assertThat(guardado.numeroReserva()).isEqualTo(reservado.numeroReserva());
        // AND sigue ocupando la agenda con el mismo margen: un turno a las 10:35 en el mismo sillón se rechaza
        assertThatThrownBy(() -> agenda.crearTurno(relojALas(8, 0))
                .ejecutar(new CrearTurnoCommand(2L, 2L, SILLON_1, CONSULTA, a(10, 35))))
                .isInstanceOf(TurnoRechazadoException.class);
    }

    @Test
    @DisplayName("Confirmar un turno que no está reservado")
    void confirmarUnTurnoQueNoEstaReservado() {
        // GIVEN un turno CANCELADO, ATENDIDO o AUSENTE
        int hora = 9;
        for (EstadoTurno estado : new EstadoTurno[] {EstadoTurno.CANCELADO, EstadoTurno.ATENDIDO, EstadoTurno.AUSENTE}) {
            Turno turno = agenda.dePacienteUno(a(hora++, 0), estado);

            // WHEN se intenta confirmarlo
            // THEN la operación se rechaza y el turno queda sin cambios
            assertThatThrownBy(() -> confirmarTurno.ejecutar(turno.id()))
                    .as("confirmar un turno %s", estado)
                    .isInstanceOf(TurnoRechazadoException.class);
            assertThat(agenda.turnos.porId(turno.id())).isEqualTo(turno);
        }
    }

    @Test
    void unTurnoYaConfirmadoNoSeConfirmaDeNuevo() {
        Turno confirmado = agenda.dePacienteUno(a(10, 0), EstadoTurno.CONFIRMADO);

        assertThatThrownBy(() -> confirmarTurno.ejecutar(confirmado.id()))
                .isInstanceOf(TurnoRechazadoException.class);
        assertThat(agenda.turnos.porId(confirmado.id())).isEqualTo(confirmado);
    }

    @Test
    void unTurnoInexistenteSeInformaComoTal() {
        assertThatThrownBy(() -> confirmarTurno.ejecutar(99L)).isInstanceOf(TurnoInexistenteException.class);
    }
}
