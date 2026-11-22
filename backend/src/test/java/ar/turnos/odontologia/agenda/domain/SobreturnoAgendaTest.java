package ar.turnos.odontologia.agenda.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Excepción del sobreturno (RN-AG-06): solo se relaja la agenda del profesional. */
class SobreturnoAgendaTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Long DRA_A = 1L;
    private static final Long DR_B = 2L;
    private static final Long SILLON_1 = 1L;
    private static final Long SILLON_2 = 2L;
    private static final Long PACIENTE = 1L;
    private static final Long CONSULTA = 1L;

    private final Agenda agenda = new Agenda(Margen.PREDETERMINADO);

    @Test
    @DisplayName("Sobreturno que pisa la agenda del profesional en otro sillón")
    void sobreturnoPisaLaAgendaDelProfesionalEnOtroSillon() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1 y el Sillón 2 libre
        Turno existente = turno(1L, DRA_A, SILLON_1, 10, 0, 10, 30);
        Turno nuevo = turno(2L, DRA_A, SILLON_2, 10, 15, 10, 45);

        // WHEN se verifica el sobreturno solo contra la restricción del sillón
        // THEN se acepta: los dos turnos de la Dra. A siguen activos
        assertThatCode(() -> agenda.verificarSillon(nuevo, List.of(existente))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sobreturno que pisa el sillón se rechaza")
    void sobreturnoQuePisaElSillonSeRechaza() {
        // GIVEN un turno activo de 10:00 a 10:30 con el Dr. B en el Sillón 1
        Turno existente = turno(1L, DR_B, SILLON_1, 10, 0, 10, 30);
        Turno nuevo = turno(2L, DRA_A, SILLON_1, 10, 10, 10, 40);

        // WHEN se verifica un sobreturno que solo tiene libre el Sillón 1
        // THEN se rechaza con SIN_SILLON_DISPONIBLE
        assertThatThrownBy(() -> agenda.verificarSillon(nuevo, List.of(existente)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    @Test
    @DisplayName("La restricción del sillón incluye a los sobreturnos")
    void laRestriccionDelSillonIncluyeASobreturnos() {
        // GIVEN un sobreturno activo de 10:00 a 10:30 en el Sillón 1
        Turno existente = Turno.sobreturno(PACIENTE, DR_B, SILLON_1, CONSULTA,
                new Intervalo(a(10, 0), a(10, 30)), new NumeroReserva("AAAA-AAAA"), "Urgencia ficticia");
        Turno nuevo = turno(2L, DRA_A, SILLON_1, 10, 25, 10, 55);

        // WHEN se intenta otro turno activo en el mismo sillón
        // THEN se rechaza: la limpieza del sillón no exceptúa a los sobreturnos
        assertThatThrownBy(() -> agenda.verificar(nuevo, List.of(existente)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SOLAPAMIENTO_SILLON);
    }

    @Test
    @DisplayName("El margen de limpieza del sillón usa el margen vigente de 15 minutos")
    void margenDeQuinceMinutosDelSillon() {
        Agenda conMargenMayor = new Agenda(Margen.deMinutos(15));
        Turno existente = turno(1L, DR_B, SILLON_1, 10, 0, 10, 30);
        Turno nuevo = turno(2L, DRA_A, SILLON_1, 10, 40, 11, 10);

        assertThatThrownBy(() -> conMargenMayor.verificarSillon(nuevo, List.of(existente)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    @Test
    @DisplayName("Un turno cancelado en el sillón no bloquea el sobreturno")
    void turnoCanceladoNoBloqueaElSillon() {
        Turno cancelado = turno(1L, DR_B, SILLON_1, 10, 0, 10, 30).cancelado();
        Turno nuevo = turno(2L, DRA_A, SILLON_1, 10, 15, 10, 45);

        assertThatCode(() -> agenda.verificarSillon(nuevo, List.of(cancelado))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un sobreturno contiguo sin margen de limpieza se rechaza")
    void sobreturnoContiguoSinMargenSeRechaza() {
        Turno existente = turno(1L, DR_B, SILLON_1, 10, 0, 10, 30);
        Turno nuevo = turno(2L, DRA_A, SILLON_1, 10, 30, 11, 0);

        assertThatThrownBy(() -> agenda.verificarSillon(nuevo, List.of(existente)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    private static Turno turno(long id, long profesional, long sillon, int desdeHora, int desdeMinuto,
                               int hastaHora, int hastaMinuto) {
        return new Turno(id, PACIENTE, profesional, sillon, CONSULTA,
                new Intervalo(a(desdeHora, desdeMinuto), a(hastaHora, hastaMinuto)),
                EstadoTurno.RESERVADO, new NumeroReserva("K7M2-QX9P"));
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
