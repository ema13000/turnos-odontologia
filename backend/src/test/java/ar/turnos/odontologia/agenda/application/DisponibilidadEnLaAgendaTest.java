package ar.turnos.odontologia.agenda.application;

import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.BUENOS_AIRES;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.CONSULTA;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DNI_PACIENTE_UNO;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DRA_A;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DR_B;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.PACIENTE_UNO;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_1;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.SILLON_2;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.a;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.relojALas;
import static ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion.Tipo.AUSENCIA;
import static ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion.Tipo.BLOQUEO;
import static ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion.Tipo.LICENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Un test por escenario del delta de {@code agenda-turnos} de C-07: horario de trabajo, ausencias, licencias y
 * bloqueos en la creación, la reprogramación, el sobreturno y la extensión, con una disponibilidad en memoria.
 * El 2026-12-01 es martes.
 */
class DisponibilidadEnLaAgendaTest {

    private static final Clock RELOJ = relojALas(8, 0);

    private final AgendaEnMemoria agenda = new AgendaEnMemoria();
    private final DisponibilidadEnMemoria disponibilidad = new DisponibilidadEnMemoria();

    @Nested
    class HorarioDeTrabajo {

        @Test
        @DisplayName("Turno dentro de la franja")
        void turnoDentroDeLaFranja() {
            // GIVEN la Dra. A con una franja el martes de 08:00 a 13:00
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0));

            // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 el martes a las 12:30
            Turno turno = crear(DRA_A, SILLON_1, a(12, 30));

            // THEN el turno se crea en estado RESERVADO, terminando justo en el fin de la franja
            assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
            assertThat(turno.intervalo().fin()).isEqualTo(a(13, 0));
        }

        @Test
        @DisplayName("Turno que termina después de la franja")
        void turnoQueTerminaDespuesDeLaFranja() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0));

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(12, 45)), CodigoRechazo.FUERA_DE_HORARIO);
            assertThat(agenda.turnos.todos()).isEmpty();
        }

        @Test
        @DisplayName("Turno en un día sin franjas")
        void turnoEnUnDiaSinFranjas() {
            // GIVEN la Dra. A con franjas solo de lunes a viernes
            disponibilidad.deLunesAViernes(DRA_A, hora(8, 0), hora(13, 0));

            // WHEN se crea un turno el sábado 2026-12-05 a las 10:00
            Instant sabado = LocalDate.of(2026, 12, 5).atTime(10, 0).atZone(BUENOS_AIRES).toInstant();

            // THEN se rechaza con FUERA_DE_HORARIO
            assertRechazo(() -> crear(DRA_A, SILLON_1, sabado), CodigoRechazo.FUERA_DE_HORARIO);
        }

        @Test
        @DisplayName("Turno que cruza el corte entre dos franjas")
        void turnoQueCruzaElCorteEntreDosFranjas() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(12, 0)).franja(DRA_A, 2, hora(16, 0), hora(20, 0));

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(11, 45)), CodigoRechazo.FUERA_DE_HORARIO);
        }

        @Test
        @DisplayName("Turno en la segunda franja del día")
        void turnoEnLaSegundaFranjaDelDia() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(12, 0)).franja(DRA_A, 2, hora(16, 0), hora(20, 0));

            Turno turno = crear(DRA_A, SILLON_1, a(16, 0));

            assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        }

        @Test
        @DisplayName("Profesional sin horarios cargados")
        void profesionalSinHorariosCargados() {
            // GIVEN el Dr. B sin ninguna franja de trabajo (la Dra. A sí tiene)
            disponibilidad.deLunesAViernes(DRA_A, hora(8, 0), hora(13, 0));

            assertRechazo(() -> crear(DR_B, SILLON_1, a(10, 0)), CodigoRechazo.FUERA_DE_HORARIO);
        }

        @Test
        @DisplayName("Reprogramar fuera de horario")
        void reprogramarFueraDeHorario() {
            // GIVEN la Dra. A con una franja el martes de 08:00 a 13:00
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0));
            // AND un turno activo de "Paciente Uno" de 10:00 a 10:30 con la Dra. A
            Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

            // WHEN el personal lo reprograma a las 14:00 del mismo día
            assertRechazo(() -> new ReprogramarTurno(agenda.turnos, crearTurno()).ejecutar(
                    new ReprogramarTurnoCommand(original.id(), DNI_PACIENTE_UNO, original.numeroReserva().valor(),
                            a(14, 0), null, null)),
                    CodigoRechazo.FUERA_DE_HORARIO);

            // AND el turno original sigue en estado RESERVADO
            assertThat(agenda.turnos.porId(original.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
            assertThat(agenda.turnos.todos()).hasSize(1);
        }
    }

    @Nested
    class AusenciasYLicencias {

        @Test
        @DisplayName("Turno durante una licencia")
        void turnoDuranteUnaLicencia() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0))
                    .periodo(DRA_A, LICENCIA, a(0, 0), a(0, 0).plusSeconds(24 * 3600));

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(10, 0)), CodigoRechazo.PROFESIONAL_AUSENTE);
        }

        @Test
        @DisplayName("Turno que termina justo cuando empieza la ausencia")
        void turnoQueTerminaJustoCuandoEmpiezaLaAusencia() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0))
                    .periodo(DRA_A, AUSENCIA, a(11, 0), a(13, 0));

            Turno turno = crear(DRA_A, SILLON_1, a(10, 30));

            assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        }

        @Test
        @DisplayName("Sobreturno durante una ausencia")
        void sobreturnoDuranteUnaAusencia() {
            disponibilidad.periodo(DRA_A, AUSENCIA, a(10, 0), a(12, 0));

            assertRechazo(() -> sobreturno(DRA_A, a(10, 30)), CodigoRechazo.PROFESIONAL_AUSENTE);
            assertThat(agenda.turnos.todos()).isEmpty();
        }
    }

    @Nested
    class Bloqueos {

        @Test
        @DisplayName("Turno durante un bloqueo")
        void turnoDuranteUnBloqueo() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0))
                    .periodo(DRA_A, BLOQUEO, a(11, 0), a(12, 0));

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(10, 45)), CodigoRechazo.AGENDA_BLOQUEADA);
        }

        @Test
        @DisplayName("Extensión que entra en un bloqueo")
        void extensionQueEntraEnUnBloqueo() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0))
                    .periodo(DRA_A, BLOQUEO, a(11, 0), a(12, 0));
            Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

            assertRechazo(() -> extender(original, a(11, 15)), CodigoRechazo.AGENDA_BLOQUEADA);

            // AND el turno sigue terminando a las 10:30
            assertThat(agenda.turnos.porId(original.id()).intervalo().fin()).isEqualTo(a(10, 30));
        }
    }

    @Nested
    class SobreturnoYExtensionFueraDeHorario {

        @Test
        @DisplayName("Sobreturno fuera de horario")
        void sobreturnoFueraDeHorario() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0));

            Turno turno = sobreturno(DRA_A, a(13, 30));

            assertThat(turno.esSobreturno()).isTrue();
            assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        }

        @Test
        @DisplayName("Extensión que pasa el fin de la franja")
        void extensionQuePasaElFinDeLaFranja() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0));
            Turno original = agenda.dePacienteUno(a(12, 30), EstadoTurno.RESERVADO);

            Turno extendido = extender(original, a(13, 20));

            assertThat(extendido.intervalo().fin()).isEqualTo(a(13, 20));
        }
    }

    @Nested
    class PrioridadDeErrores {

        @Test
        @DisplayName("Choque por profesional y por sillón a la vez")
        void choquePorProfesionalYPorSillonALaVez() {
            // Con las reglas de C-07 conectadas y la Dra. A dentro de su horario
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0));
            agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(10, 15)), CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
        }

        @Test
        @DisplayName("Ausencia y solapamiento a la vez")
        void ausenciaYSolapamientoALaVez() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(13, 0))
                    .periodo(DRA_A, AUSENCIA, a(10, 0), a(12, 0));
            agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(10, 15)), CodigoRechazo.PROFESIONAL_AUSENTE);
        }

        @Test
        @DisplayName("Fuera de horario y solapamiento a la vez")
        void fueraDeHorarioYSolapamientoALaVez() {
            disponibilidad.franja(DRA_A, 2, hora(8, 0), hora(10, 30));
            agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

            assertRechazo(() -> crear(DRA_A, SILLON_1, a(10, 15)), CodigoRechazo.FUERA_DE_HORARIO);
        }
    }

    private CrearTurno crearTurno() {
        return new CrearTurno(agenda.turnos, agenda.recursos, agenda.numeros, RELOJ, Margen.PREDETERMINADO,
                disponibilidad, BUENOS_AIRES);
    }

    private Turno crear(Long profesional, Long sillon, Instant inicio) {
        return crearTurno().ejecutar(new CrearTurnoCommand(PACIENTE_UNO, profesional, sillon, CONSULTA, inicio));
    }

    private Turno sobreturno(Long profesional, Instant inicio) {
        CrearSobreturno caso = new CrearSobreturno(agenda.turnos, agenda.recursos,
                new SillonesDisponiblesEnMemoria(agenda.recursos, SILLON_1, SILLON_2), agenda.numeros, RELOJ,
                Margen.PREDETERMINADO, disponibilidad, BUENOS_AIRES);
        return caso.ejecutar(new CrearSobreturnoCommand(PACIENTE_UNO, profesional, CONSULTA, inicio,
                "Urgencia ficticia"));
    }

    private Turno extender(Turno original, Instant fin) {
        return new ExtenderTurno(agenda.turnos, crearTurno())
                .ejecutar(new ExtenderTurnoCommand(original.id(), fin, "Complicación ficticia"));
    }

    private static LocalTime hora(int h, int m) {
        return LocalTime.of(h, m);
    }

    private static void assertRechazo(ThrowingCallable accion, CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting("codigo")
                .isEqualTo(esperado);
    }
}
