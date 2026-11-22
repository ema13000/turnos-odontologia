package ar.turnos.odontologia.agenda.application;

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
import java.time.ZoneId;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Un test por escenario del spec {@code agenda-turnos}: alta de sobreturnos (RN-AG-06). */
class CrearSobreturnoTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate DIA = LocalDate.of(2026, 12, 1);
    private static final Long PACIENTE = 1L;
    private static final Long DRA_A = 1L;
    private static final Long DR_B = 2L;
    private static final Long SILLON_1 = 1L;
    private static final Long SILLON_2 = 2L;
    private static final Long CONSULTA = 1L;
    private static final Clock RELOJ = relojALas(8, 0);

    private final AgendaEnMemoria fixture = new AgendaEnMemoria();
    private SillonesDisponiblesEnMemoria sillones = new SillonesDisponiblesEnMemoria(fixture.recursos, SILLON_1, SILLON_2);

    @Test
    @DisplayName("Sobreturno con motivo válido")
    void sobreturnoConMotivoValido() {
        // GIVEN una agenda sin otros turnos y un motivo administrativo "Urgencia con el Paciente Uno"
        // WHEN el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:00
        Turno turno = crear(DRA_A, a(10, 0), "Urgencia con el Paciente Uno");

        // THEN el turno se crea en estado RESERVADO con esSobreturno verdadero
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(turno.esSobreturno()).isTrue();
        // AND el motivo queda asociado al turno, sin exponerse en la respuesta
        assertThat(turno.motivoSobreturno()).isEqualTo("Urgencia con el Paciente Uno");
        assertThat(turno.intervalo().fin()).isEqualTo(a(10, 30));
        assertThat(turno.sillonId()).isEqualTo(SILLON_1);
    }

    @Test
    @DisplayName("Sobreturno sin motivo")
    void sobreturnoSinMotivo() {
        // GIVEN el personal con sesión iniciada
        // WHEN crea un sobreturno con el motivo vacío o solo espacios
        assertRechazo(() -> crear(DRA_A, a(10, 0), ""), CodigoRechazo.DATOS_INVALIDOS);
        assertRechazo(() -> crear(DRA_A, a(10, 0), "   "), CodigoRechazo.DATOS_INVALIDOS);

        // THEN se rechaza con DATOS_INVALIDOS y no se crea ningún turno
        assertThat(fixture.turnos.todos()).isEmpty();
    }

    @Test
    @DisplayName("Elige el sillón libre en orden determinista")
    void eligeElSillonLibreEnOrdenDeterminista() {
        // GIVEN el Sillón 1 y el Sillón 2 activos y libres, sin información de sillón habitual disponible
        // WHEN el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:00
        Turno primero = crear(DRA_A, a(10, 0), "Urgencia ficticia");

        // THEN el sillón asignado es el Sillón 1
        assertThat(primero.sillonId()).isEqualTo(SILLON_1);
        // AND una operación idéntica repetida más tarde vuelve a elegir el Sillón 1 si sigue libre
        Turno segundo = crear(DRA_A, a(11, 0), "Urgencia ficticia");
        assertThat(segundo.sillonId()).isEqualTo(SILLON_1);
    }

    @Test
    @DisplayName("Usa otro sillón cuando el primero está ocupado")
    void usaOtroSillonCuandoElPrimeroEstaOcupado() {
        // GIVEN un turno activo de 10:00 a 10:30 con el Dr. B en el Sillón 1
        fixture.existente(2L, DR_B, SILLON_1, a(10, 0), EstadoTurno.RESERVADO);
        // AND el Sillón 2 activo y libre

        // WHEN el personal crea un sobreturno de "Consulta" con la Dra. A a las 10:10
        Turno turno = crear(DRA_A, a(10, 10), "Urgencia ficticia");

        // THEN el sobreturno se crea en el Sillón 2 con fin 10:40
        assertThat(turno.sillonId()).isEqualTo(SILLON_2);
        assertThat(turno.intervalo().fin()).isEqualTo(a(10, 40));
        assertThat(turno.esSobreturno()).isTrue();
    }

    @Test
    @DisplayName("Sin sillón libre con margen de limpieza")
    void sinSillonLibreConMargenDeLimpieza() {
        // GIVEN turnos activos de 10:00 a 10:30 en el Sillón 1 y en el Sillón 2, con margen de 10 minutos
        fixture.existente(2L, DR_B, SILLON_1, a(10, 0), EstadoTurno.RESERVADO);
        fixture.existente(3L, DR_B, SILLON_2, a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal crea un sobreturno de "Consulta" a las 10:35
        assertRechazo(() -> crear(DRA_A, a(10, 35), "Urgencia ficticia"), CodigoRechazo.SIN_SILLON_DISPONIBLE);

        // THEN no se crea ningún turno
        assertThat(fixture.turnos.todos()).hasSize(2);
    }

    @Test
    @DisplayName("El margen de limpieza del sillón es obligatorio")
    void elMargenDeLimpiezaDelSillonEsObligatorio() {
        // GIVEN turnos activos que terminan a las 10:30 en el Sillón 1 y en el Sillón 2
        fixture.existente(2L, DR_B, SILLON_1, a(10, 0), EstadoTurno.RESERVADO);
        fixture.existente(3L, DR_B, SILLON_2, a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal crea un sobreturno de "Consulta" a las 10:30 con margen de 10 minutos
        // THEN se rechaza con SIN_SILLON_DISPONIBLE
        assertRechazo(() -> crear(DRA_A, a(10, 30), "Urgencia ficticia"), CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    @Test
    @DisplayName("Rechazo sin efectos parciales")
    void rechazoSinEfectosParciales() {
        // GIVEN turnos activos que ocupan los dos sillones de 10:00 a 10:30
        fixture.existente(2L, DR_B, SILLON_1, a(10, 0), EstadoTurno.RESERVADO);
        fixture.existente(3L, DR_B, SILLON_2, a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal intenta crear un sobreturno a las 10:10 y la operación se rechaza
        assertRechazo(() -> crear(DRA_A, a(10, 10), "Urgencia ficticia"), CodigoRechazo.SIN_SILLON_DISPONIBLE);

        // THEN no existe un turno sobreturno nuevo
        assertThat(fixture.turnos.todos()).hasSize(2).noneMatch(Turno::esSobreturno);
        // AND no existe ningún registro de auditoría de esa operación (el borde no registró nada)
    }

    @Test
    @DisplayName("Un sillón inactivo se excluye de la selección")
    void sillonInactivoSeExcluye() {
        fixture.recursos.desactivarSillon(SILLON_1);

        Turno turno = crear(DRA_A, a(10, 0), "Urgencia ficticia");

        assertThat(turno.sillonId()).isEqualTo(SILLON_2);
    }

    @Test
    @DisplayName("El margen de 15 minutos también se aplica al sobreturno")
    void margenDeQuinceMinutos() {
        // Con margen de 15, un sillón que termina a las 10:30 no sirve para un inicio a las 10:40.
        fixture.existente(2L, DR_B, SILLON_1, a(10, 0), EstadoTurno.RESERVADO);
        fixture.existente(3L, DR_B, SILLON_2, a(10, 0), EstadoTurno.RESERVADO);
        CrearSobreturno caso = casoDeUso(RELOJ, Margen.deMinutos(15));

        assertThatThrownBy(() -> caso.ejecutar(comando(DRA_A, a(10, 40), "Urgencia ficticia")))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    @Test
    @DisplayName("El motivo del sobreturno admite 500 caracteres pero no 501")
    void motivoValidaLongitud() {
        Turno turno = crear(DRA_A, a(10, 0), "a".repeat(500));
        assertThat(turno.motivoSobreturno()).hasSize(500);

        assertRechazo(() -> crear(DRA_A, a(12, 0), "a".repeat(501)), CodigoRechazo.DATOS_INVALIDOS);
        assertThat(fixture.turnos.todos()).hasSize(1);
    }

    @Test
    @DisplayName("Un solapamiento detectado por la base se traduce a SIN_SILLON_DISPONIBLE solo en sobreturnos")
    void traduccionLocalDelSolapamiento() {
        TurnoRepository queChoca = new TurnoRepositoryEnMemoria() {
            @Override
            public Turno guardar(Turno turno) {
                throw new TurnoRechazadoException(CodigoRechazo.SOLAPAMIENTO_SILLON, "choque de la base");
            }
        };
        CrearSobreturno caso = new CrearSobreturno(queChoca, fixture.recursos, sillones, fixture.numeros,
                RELOJ, Margen.PREDETERMINADO);

        assertRechazo(() -> caso.ejecutar(comando(DRA_A, a(10, 0), "Urgencia ficticia")),
                CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    @Test
    @DisplayName("Un error de margen detectado por la base también se traduce a SIN_SILLON_DISPONIBLE")
    void traduccionLocalDelMargen() {
        TurnoRepository queChoca = new TurnoRepositoryEnMemoria() {
            @Override
            public Turno guardar(Turno turno) {
                throw new TurnoRechazadoException(CodigoRechazo.MARGEN_INSUFICIENTE, "margen de la base");
            }
        };
        CrearSobreturno caso = new CrearSobreturno(queChoca, fixture.recursos, sillones, fixture.numeros,
                RELOJ, Margen.PREDETERMINADO);

        assertRechazo(() -> caso.ejecutar(comando(DRA_A, a(10, 0), "Urgencia ficticia")),
                CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    @Test
    @DisplayName("Un sillón tomado en una carrera se descarta y se prueba el siguiente")
    void sillonTomadoEnUnaCarreraSeDescarta() {
        // GIVEN el Sillón 1 y el Sillón 2 libres, y el Sillón 1 tomado por otra operación tras la verificación
        TurnoRepository queChoca = new TurnoRepositoryEnMemoria() {
            @Override
            public Turno guardar(Turno turno) {
                if (SILLON_1.equals(turno.sillonId())) {
                    throw new TurnoRechazadoException(CodigoRechazo.SOLAPAMIENTO_SILLON, "choque de la base");
                }
                return super.guardar(turno);
            }
        };
        CrearSobreturno caso = new CrearSobreturno(queChoca, fixture.recursos, sillones, fixture.numeros,
                RELOJ, Margen.PREDETERMINADO);
        CrearSobreturnoCommand comando = comando(DRA_A, a(10, 0), "Urgencia ficticia");

        // WHEN el caso de uso informa el sillón tomado, para que quien lo invoca reintente sin él
        assertThatThrownBy(() -> caso.ejecutar(comando))
                .isInstanceOf(SillonTomadoException.class)
                .hasFieldOrPropertyWithValue("sillonId", SILLON_1)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE);

        // THEN el reintento sin el Sillón 1 crea el sobreturno en el Sillón 2
        Turno turno = caso.ejecutar(comando, Set.of(SILLON_1));
        assertThat(turno.sillonId()).isEqualTo(SILLON_2);
    }

    @Test
    @DisplayName("Con todos los sillones descartados se rechaza con SIN_SILLON_DISPONIBLE")
    void conTodosLosSillonesDescartados() {
        CrearSobreturno caso = casoDeUso(RELOJ);

        assertRechazo(() -> caso.ejecutar(comando(DRA_A, a(10, 0), "Urgencia ficticia"),
                Set.of(SILLON_1, SILLON_2)), CodigoRechazo.SIN_SILLON_DISPONIBLE);
        assertThat(fixture.turnos.todos()).isEmpty();
    }

    @Test
    @DisplayName("Sin sillón habitual, dos profesionales con el mismo conjunto eligen el Sillón 1 por id")
    void seleccionDeterministaSinSillonHabitual() {
        Turno primero = crear(DRA_A, a(10, 0), "Urgencia ficticia");
        Turno segundo = crear(DR_B, a(11, 0), "Urgencia ficticia");

        assertThat(primero.sillonId()).isEqualTo(SILLON_1);
        assertThat(segundo.sillonId()).isEqualTo(SILLON_1);
        assertThat(sillones.consultasPreferido()).isEqualTo(2);
    }

    @Test
    @DisplayName("El enganche del sillón preferido se usa cuando está libre con margen")
    void sillonesPreferidoSeUsaSiEstaLibre() {
        sillones.preferir(SILLON_2);

        Turno turno = crear(DRA_A, a(10, 0), "Urgencia ficticia");

        assertThat(turno.sillonId()).isEqualTo(SILLON_2);
    }

    @Test
    @DisplayName("Si el sillón preferido está ocupado se cae al orden determinista")
    void sillonesPreferidoOcupadoCaeAlOrden() {
        sillones.preferir(SILLON_1);
        fixture.existente(2L, DR_B, SILLON_1, a(10, 0), EstadoTurno.RESERVADO);

        Turno turno = crear(DRA_A, a(10, 10), "Urgencia ficticia");

        assertThat(turno.sillonId()).isEqualTo(SILLON_2);
    }

    private static Instant a(int hora, int minuto) {
        return DIA.atTime(LocalTime.of(hora, minuto)).atZone(BUENOS_AIRES).toInstant();
    }

    private static Clock relojALas(int hora, int minuto) {
        return Clock.fixed(a(hora, minuto), BUENOS_AIRES);
    }

    private CrearSobreturno casoDeUso(Clock reloj) {
        return casoDeUso(reloj, Margen.PREDETERMINADO);
    }

    private CrearSobreturno casoDeUso(Clock reloj, Margen margen) {
        return new CrearSobreturno(fixture.turnos, fixture.recursos, sillones, fixture.numeros, reloj, margen);
    }

    private Turno crear(Long profesional, Instant inicio, String motivo) {
        return casoDeUso(RELOJ).ejecutar(comando(profesional, inicio, motivo));
    }

    private static CrearSobreturnoCommand comando(Long profesional, Instant inicio, String motivo) {
        return new CrearSobreturnoCommand(PACIENTE, profesional, CONSULTA, inicio, motivo);
    }

    private static void assertRechazo(ThrowingCallable accion, CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(esperado);
    }
}
