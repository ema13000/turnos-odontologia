package ar.turnos.odontologia.agenda.application;

import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.BUENOS_AIRES;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.CONSULTA;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DRA_A;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DR_B;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.PACIENTE_UNO;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_1;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.a;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.relojALas;
import static ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion.Tipo.LICENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reasignación de un turno afectado por una ausencia (C-07, design D10), sin base. */
class ReasignarTurnoTest {

    private final AgendaEnMemoria agenda = new AgendaEnMemoria();
    private final DisponibilidadEnMemoria disponibilidad = new DisponibilidadEnMemoria();
    /** Pares "profesional:prestación" habilitados (RN-AG-15). */
    private final Set<String> habilitados = new HashSet<>();

    private Turno original;

    @BeforeEach
    void prepararTurnoAfectado() {
        disponibilidad.deLunesAViernes(DRA_A, LocalTime.of(8, 0), LocalTime.of(13, 0))
                .deLunesAViernes(DR_B, LocalTime.of(8, 0), LocalTime.of(18, 0))
                .periodo(DRA_A, LICENCIA, a(0, 0), a(0, 0).plusSeconds(24 * 3600));
        habilitados.add(DR_B + ":" + CONSULTA);
        original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
    }

    @Test
    @DisplayName("Reasigna al mismo horario con otro profesional")
    void mismoHorarioConOtroProfesional() {
        ReprogramarTurno.Reprogramacion resultado = reasignar(DR_B, null);

        assertThat(resultado.original().estado()).isEqualTo(EstadoTurno.CANCELADO);
        Turno nuevo = resultado.nuevo();
        assertThat(nuevo.profesionalId()).isEqualTo(DR_B);
        assertThat(nuevo.pacienteId()).isEqualTo(PACIENTE_UNO);
        assertThat(nuevo.sillonId()).isEqualTo(SILLON_1);
        assertThat(nuevo.intervalo()).isEqualTo(original.intervalo());
        assertThat(nuevo.numeroReserva()).isNotEqualTo(original.numeroReserva());
        assertThat(nuevo.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    @DisplayName("Reasigna a otro horario con el mismo profesional")
    void otroHorarioConElMismoProfesional() {
        // el miércoles siguiente, después de la licencia del martes
        Instant miercoles = a(10, 0).plusSeconds(24 * 3600);

        Turno nuevo = reasignar(null, miercoles).nuevo();

        assertThat(nuevo.profesionalId()).isEqualTo(DRA_A);
        assertThat(nuevo.intervalo().inicio()).isEqualTo(miercoles);
        assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.CANCELADO);
    }

    @Test
    @DisplayName("Rechaza un profesional no habilitado para la prestación")
    void profesionalNoHabilitado() {
        habilitados.clear();

        assertRechazo(() -> reasignar(DR_B, null), CodigoRechazo.PROFESIONAL_NO_HABILITADO);
        assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(agenda.turnos.todos()).hasSize(1);
    }

    @Test
    @DisplayName("Rechaza un turno que no está activo")
    void turnoNoActivo() {
        Turno cancelado = agenda.dePacienteUno(a(12, 0), EstadoTurno.CANCELADO);

        assertRechazo(() -> casoDeUso().ejecutar(new ReasignarTurnoCommand(cancelado.id(), DR_B, null, null)),
                CodigoRechazo.TURNO_NO_ACTIVO);
    }

    @Test
    @DisplayName("Rechaza un nuevo horario que cae en la ausencia y deja el original intacto")
    void nuevoHorarioEnLaAusencia() {
        assertRechazo(() -> reasignar(null, a(11, 0)), CodigoRechazo.PROFESIONAL_AUSENTE);

        assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(agenda.turnos.todos()).hasSize(1);
    }

    @Test
    @DisplayName("Solo cancela el original si el turno nuevo es válido")
    void soloCancelaSiElNuevoEsValido() {
        // el Dr. B está habilitado, pero el nuevo horario queda fuera de su franja
        assertRechazo(() -> reasignar(DR_B, a(18, 0)), CodigoRechazo.FUERA_DE_HORARIO);

        assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(agenda.turnos.todos()).hasSize(1);
    }

    private ReasignarTurno casoDeUso() {
        CrearTurno crearTurno = new CrearTurno(agenda.turnos, agenda.recursos, agenda.numeros, relojALas(8, 0),
                Margen.PREDETERMINADO, disponibilidad, BUENOS_AIRES);
        return new ReasignarTurno(agenda.turnos, crearTurno,
                (profesional, prestacion) -> habilitados.contains(profesional + ":" + prestacion));
    }

    private ReprogramarTurno.Reprogramacion reasignar(Long profesional, Instant inicio) {
        return casoDeUso().ejecutar(new ReasignarTurnoCommand(original.id(), profesional, inicio, null));
    }

    private static void assertRechazo(ThrowingCallable accion, CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting("codigo")
                .isEqualTo(esperado);
    }
}
