package ar.turnos.odontologia.agenda.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Un test por escenario del spec {@code agenda-turnos} (change crear-turno-sin-solapamientos).
 * Datos ficticios: Dra. A, Dr. B, Sillón 1 y Sillón 2, prestación "Consulta" de 30 minutos,
 * margen de 10 minutos, día 2026-12-01 y momento actual a las 08:00 en Buenos Aires.
 */
class CrearTurnoTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate DIA = LocalDate.of(2026, 12, 1);

    private static final UUID PACIENTE = UUID.randomUUID();
    private static final UUID DRA_A = UUID.randomUUID();
    private static final UUID DR_B = UUID.randomUUID();
    private static final UUID SILLON_1 = UUID.randomUUID();
    private static final UUID SILLON_2 = UUID.randomUUID();
    private static final UUID CONSULTA = UUID.randomUUID();

    private TurnoRepositoryEnMemoria turnos;
    private PrestacionRepositoryEnMemoria prestaciones;

    @BeforeEach
    void prepararAgenda() {
        turnos = new TurnoRepositoryEnMemoria();
        prestaciones = new PrestacionRepositoryEnMemoria();
        prestaciones.agregar(CONSULTA, Duration.ofMinutes(30));
    }

    // ---------- Requirement: Intervalo del turno ----------

    @Test
    @DisplayName("Turno sin conflictos")
    void turnoSinConflictos() {
        // GIVEN una agenda sin turnos
        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
        Turno turno = crear(DRA_A, SILLON_1, a(10, 0));

        // THEN el turno se crea en estado RESERVADO, AND su fin es las 10:30
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(turno.intervalo().inicio()).isEqualTo(a(10, 0));
        assertThat(turno.intervalo().fin()).isEqualTo(a(10, 30));
        assertThat(turnos.todos()).containsExactly(turno);
    }

    // ---------- Requirement: Solo los turnos activos ocupan la agenda ----------

    @Test
    @DisplayName("Un turno cancelado no bloquea el horario")
    void unTurnoCanceladoNoBloquea() {
        // GIVEN un turno CANCELADO de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existente(DRA_A, SILLON_1, a(10, 0), a(10, 30), EstadoTurno.CANCELADO);

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:00
        Turno turno = crear(DRA_A, SILLON_1, a(10, 0));

        // THEN el turno se crea en estado RESERVADO
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    // ---------- Requirement: Sin superposición por profesional ----------

    @Test
    @DisplayName("El profesional ya atiende en otro sillón")
    void elProfesionalYaAtiendeEnOtroSillon() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 2 a las 10:15
        // THEN el turno se rechaza con SOLAPAMIENTO_PROFESIONAL
        assertRechazo(() -> crear(DRA_A, SILLON_2, a(10, 15)), CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
    }

    // ---------- Requirement: Sin superposición por sillón ----------

    @Test
    @DisplayName("El sillón ya está ocupado por otro profesional")
    void elSillonYaEstaOcupado() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con el Dr. B en el Sillón 1 a las 10:15
        // THEN el turno se rechaza con SOLAPAMIENTO_SILLON
        assertRechazo(() -> crear(DR_B, SILLON_1, a(10, 15)), CodigoRechazo.SOLAPAMIENTO_SILLON);
    }

    // ---------- Requirement: Margen entre turnos que comparten recurso ----------

    @Test
    @DisplayName("Turno contiguo sin margen")
    void turnoContiguoSinMargen() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 2 a las 10:30
        // THEN el turno se rechaza con MARGEN_INSUFICIENTE
        assertRechazo(() -> crear(DRA_A, SILLON_2, a(10, 30)), CodigoRechazo.MARGEN_INSUFICIENTE);
    }

    @Test
    @DisplayName("Turno que respeta el margen exacto")
    void turnoQueRespetaElMargenExacto() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:40
        Turno turno = crear(DRA_A, SILLON_1, a(10, 40));

        // THEN el turno se crea en estado RESERVADO
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    @DisplayName("Turno contiguo sin recursos compartidos")
    void turnoContiguoSinRecursosCompartidos() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con el Dr. B en el Sillón 2 a las 10:30
        Turno turno = crear(DR_B, SILLON_2, a(10, 30));

        // THEN el turno se crea en estado RESERVADO
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    // ---------- Requirement: Margen configurable con un mínimo de 10 minutos ----------
    // El escenario "Margen menor al mínimo" está en MargenTest.

    @Test
    @DisplayName("Margen mayor al mínimo")
    void margenMayorAlMinimo() {
        // GIVEN un margen configurado de 15 minutos
        CrearTurno crearTurno = casoDeUso(relojALas(8, 0), Margen.deMinutos(15));
        // AND un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:40
        // THEN el turno se rechaza con MARGEN_INSUFICIENTE
        assertRechazo(() -> crearTurno.ejecutar(comando(DRA_A, SILLON_1, a(10, 40))),
                CodigoRechazo.MARGEN_INSUFICIENTE);
    }

    // ---------- Requirement: Un solo error con prioridad fija ----------

    @Test
    @DisplayName("Choque por profesional y por sillón a la vez")
    void chocaPorProfesionalYPorSillon() {
        // GIVEN un turno activo de 10:00 a 10:30 con la Dra. A en el Sillón 1
        existenteActivo(DRA_A, SILLON_1, a(10, 0), a(10, 30));

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 10:15
        // THEN el turno se rechaza con SOLAPAMIENTO_PROFESIONAL
        assertRechazo(() -> crear(DRA_A, SILLON_1, a(10, 15)), CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
    }

    // ---------- Requirement: Sin turnos en el pasado ----------

    @Test
    @DisplayName("Inicio anterior al momento actual")
    void inicioAnteriorAlMomentoActual() {
        // GIVEN que el momento actual es el 2026-12-01 a las 09:00
        CrearTurno crearTurno = casoDeUso(relojALas(9, 0), Margen.PREDETERMINADO);

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 08:30
        // THEN el turno se rechaza con INICIO_EN_EL_PASADO
        assertRechazo(() -> crearTurno.ejecutar(comando(DRA_A, SILLON_1, a(8, 30))),
                CodigoRechazo.INICIO_EN_EL_PASADO);
        assertThat(turnos.todos()).isEmpty();
    }

    @Test
    @DisplayName("Inicio igual al momento actual")
    void inicioIgualAlMomentoActual() {
        // GIVEN que el momento actual es el 2026-12-01 a las 09:00, AND una agenda sin turnos
        CrearTurno crearTurno = casoDeUso(relojALas(9, 0), Margen.PREDETERMINADO);

        // WHEN se crea un turno de "Consulta" con la Dra. A en el Sillón 1 a las 09:00
        Turno turno = crearTurno.ejecutar(comando(DRA_A, SILLON_1, a(9, 0)));

        // THEN el turno se crea en estado RESERVADO
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    // ---------- Ayudas del fixture ----------

    private static Instant a(int hora, int minuto) {
        return DIA.atTime(LocalTime.of(hora, minuto)).atZone(BUENOS_AIRES).toInstant();
    }

    private static Clock relojALas(int hora, int minuto) {
        return Clock.fixed(a(hora, minuto), BUENOS_AIRES);
    }

    private CrearTurno casoDeUso(Clock reloj, Margen margen) {
        return new CrearTurno(turnos, prestaciones, reloj, margen);
    }

    /** Crea un turno de "Consulta" con el reloj a las 08:00 y el margen de 10 minutos. */
    private Turno crear(UUID profesional, UUID sillon, Instant inicio) {
        return casoDeUso(relojALas(8, 0), Margen.PREDETERMINADO).ejecutar(comando(profesional, sillon, inicio));
    }

    private static CrearTurnoCommand comando(UUID profesional, UUID sillon, Instant inicio) {
        return new CrearTurnoCommand(PACIENTE, profesional, sillon, CONSULTA, inicio);
    }

    private void existenteActivo(UUID profesional, UUID sillon, Instant inicio, Instant fin) {
        existente(profesional, sillon, inicio, fin, EstadoTurno.RESERVADO);
    }

    private void existente(UUID profesional, UUID sillon, Instant inicio, Instant fin, EstadoTurno estado) {
        turnos.guardar(new Turno(UUID.randomUUID(), UUID.randomUUID(), profesional, sillon, CONSULTA,
                new Intervalo(inicio, fin), estado));
    }

    private static void assertRechazo(ThrowingCallable accion, CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(esperado);
    }
}
