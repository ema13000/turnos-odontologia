package ar.turnos.odontologia.agenda.domain.reglas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Reglas puras de C-07 (design D1, D3 y D4). El 2026-12-01 es martes; todo en Buenos Aires. */
class ReglasDisponibilidadTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate MARTES = LocalDate.of(2026, 12, 1);
    private static final int LUNES = 1;
    private static final int MARTES_ISO = 2;

    private static Instant martes(int hora, int minuto) {
        return MARTES.atTime(hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }

    private static Intervalo turno(int hora, int minuto, int duracionMinutos) {
        Instant inicio = martes(hora, minuto);
        return new Intervalo(inicio, inicio.plusSeconds(duracionMinutos * 60L));
    }

    private static FranjaHoraria franja(int dia, int desde, int hasta) {
        return FranjaHoraria.de(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private static PeriodoSinAtencion periodo(PeriodoSinAtencion.Tipo tipo, int desde, int hasta) {
        return PeriodoSinAtencion.de(tipo, martes(desde, 0), martes(hasta, 0));
    }

    private static CodigoRechazo rechazo(Runnable verificacion) {
        try {
            verificacion.run();
        } catch (TurnoRechazadoException e) {
            return e.codigo();
        }
        return null;
    }

    @Nested
    class Franjas {

        @Test
        @DisplayName("Una franja con el fin antes o igual al inicio es HORARIO_INVALIDO")
        void franjaInvalida() {
            assertThatThrownBy(() -> franja(MARTES_ISO, 12, 8))
                    .isInstanceOf(DisponibilidadException.class)
                    .extracting("codigo").isEqualTo(CodigoDisponibilidad.HORARIO_INVALIDO);
            assertThatThrownBy(() -> franja(MARTES_ISO, 8, 8))
                    .extracting("codigo").isEqualTo(CodigoDisponibilidad.HORARIO_INVALIDO);
        }

        @Test
        @DisplayName("Un día fuera de 1 a 7 es HORARIO_INVALIDO")
        void diaInvalido() {
            assertThatThrownBy(() -> franja(0, 8, 12))
                    .extracting("codigo").isEqualTo(CodigoDisponibilidad.HORARIO_INVALIDO);
            assertThatThrownBy(() -> franja(8, 8, 12))
                    .extracting("codigo").isEqualTo(CodigoDisponibilidad.HORARIO_INVALIDO);
        }

        @Test
        @DisplayName("Dos franjas superpuestas del mismo día son FRANJAS_SUPERPUESTAS")
        void superpuestas() {
            assertThatThrownBy(() -> HorarioSemanal.de(List.of(franja(MARTES_ISO, 8, 12), franja(MARTES_ISO, 11, 14))))
                    .extracting("codigo").isEqualTo(CodigoDisponibilidad.FRANJAS_SUPERPUESTAS);
        }

        @Test
        @DisplayName("Dos franjas contiguas o en días distintos se aceptan")
        void contiguasYOtroDia() {
            assertThatCode(() -> HorarioSemanal.de(List.of(
                    franja(MARTES_ISO, 8, 12), franja(MARTES_ISO, 12, 14), franja(LUNES, 8, 12))))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class Horario {

        private final HorarioSemanal horario = HorarioSemanal.de(List.of(
                franja(MARTES_ISO, 8, 12), franja(MARTES_ISO, 16, 20)));

        @Test
        @DisplayName("Un turno que termina justo en el fin de la franja está dentro")
        void terminaEnElBorde() {
            assertThat(horario.contiene(turno(11, 30, 30), BUENOS_AIRES)).isTrue();
            assertThat(horario.contiene(turno(8, 0, 30), BUENOS_AIRES)).isTrue();
        }

        @Test
        @DisplayName("Un turno que se pasa del fin de la franja está fuera")
        void sePasa() {
            assertThat(horario.contiene(turno(11, 45, 30), BUENOS_AIRES)).isFalse();
        }

        @Test
        @DisplayName("Un turno que cruza dos franjas contiguas está fuera")
        void cruzaDosFranjas() {
            HorarioSemanal contiguas = HorarioSemanal.de(List.of(franja(MARTES_ISO, 8, 12), franja(MARTES_ISO, 12, 14)));
            assertThat(contiguas.contiene(turno(11, 45, 30), BUENOS_AIRES)).isFalse();
        }

        @Test
        @DisplayName("Un turno en un día sin franjas está fuera")
        void diaSinFranjas() {
            Instant miercoles = MARTES.plusDays(1).atTime(9, 0).atZone(BUENOS_AIRES).toInstant();
            assertThat(horario.contiene(new Intervalo(miercoles, miercoles.plusSeconds(1800)), BUENOS_AIRES)).isFalse();
        }

        @Test
        @DisplayName("Sin franjas, ningún turno está dentro de horario")
        void sinFranjas() {
            assertThat(HorarioSemanal.de(List.of()).contiene(turno(10, 0, 30), BUENOS_AIRES)).isFalse();
        }
    }

    @Nested
    class Periodos {

        @Test
        @DisplayName("Un período con el fin antes o igual al inicio es PERIODO_INVALIDO")
        void periodoInvalido() {
            assertThatThrownBy(() -> periodo(PeriodoSinAtencion.Tipo.AUSENCIA, 12, 10))
                    .extracting("codigo").isEqualTo(CodigoDisponibilidad.PERIODO_INVALIDO);
        }

        @Test
        @DisplayName("Un turno que termina justo cuando empieza el período no se cruza con él")
        void cruceExacto() {
            PeriodoSinAtencion ausencia = periodo(PeriodoSinAtencion.Tipo.AUSENCIA, 11, 13);
            assertThat(ausencia.seCruzaCon(turno(10, 30, 30))).isFalse();
            assertThat(ausencia.seCruzaCon(turno(10, 45, 30))).isTrue();
            assertThat(ausencia.seCruzaCon(turno(13, 0, 30))).isFalse();
        }
    }

    @Nested
    class Prioridad {

        private final HorarioSemanal horario = HorarioSemanal.de(List.of(franja(MARTES_ISO, 8, 13)));

        @Test
        @DisplayName("Dentro de horario y sin períodos no hay rechazo")
        void sinRechazo() {
            PeriodosProfesional periodos = new PeriodosProfesional(horario, List.of());
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(10, 0, 30), periodos, BUENOS_AIRES, true)))
                    .isNull();
        }

        @Test
        @DisplayName("La ausencia gana al bloqueo y al horario")
        void ausenciaPrimero() {
            PeriodosProfesional periodos = new PeriodosProfesional(horario, List.of(
                    periodo(PeriodoSinAtencion.Tipo.BLOQUEO, 12, 14),
                    periodo(PeriodoSinAtencion.Tipo.LICENCIA, 12, 14)));
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(12, 45, 30), periodos, BUENOS_AIRES, true)))
                    .isEqualTo(CodigoRechazo.PROFESIONAL_AUSENTE);
        }

        @Test
        @DisplayName("El bloqueo gana al horario")
        void bloqueoAntesQueHorario() {
            PeriodosProfesional periodos = new PeriodosProfesional(horario,
                    List.of(periodo(PeriodoSinAtencion.Tipo.BLOQUEO, 12, 14)));
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(12, 45, 30), periodos, BUENOS_AIRES, true)))
                    .isEqualTo(CodigoRechazo.AGENDA_BLOQUEADA);
        }

        @Test
        @DisplayName("Fuera de horario sin períodos es FUERA_DE_HORARIO")
        void fueraDeHorario() {
            PeriodosProfesional periodos = new PeriodosProfesional(horario, List.of());
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(12, 45, 30), periodos, BUENOS_AIRES, true)))
                    .isEqualTo(CodigoRechazo.FUERA_DE_HORARIO);
        }

        @Test
        @DisplayName("Sin exigir horario, un turno fuera de franja pasa, pero la ausencia y el bloqueo lo frenan")
        void sinExigirHorario() {
            PeriodosProfesional libre = new PeriodosProfesional(horario, List.of());
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(13, 30, 30), libre, BUENOS_AIRES, false)))
                    .isNull();

            PeriodosProfesional bloqueado = new PeriodosProfesional(horario,
                    List.of(periodo(PeriodoSinAtencion.Tipo.BLOQUEO, 13, 14)));
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(13, 30, 30), bloqueado, BUENOS_AIRES, false)))
                    .isEqualTo(CodigoRechazo.AGENDA_BLOQUEADA);
        }

        @Test
        @DisplayName("Sin restricciones, cualquier turno pasa")
        void sinRestricciones() {
            assertThat(rechazo(() -> ReglasDisponibilidad.verificar(turno(23, 0, 30),
                    PeriodosProfesional.sinRestricciones(), BUENOS_AIRES, true))).isNull();
        }
    }
}
