package ar.turnos.odontologia.agenda.application;

import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DRA_A;
import static ar.turnos.odontologia.agenda.application.AgendaEnMemoria.DR_B;
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
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.NumeroReserva;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Un test por escenario del spec {@code agenda-turnos}: extensión de turnos (RN-AG-14). */
class ExtenderTurnoTest {

    private static final String MOTIVO = "Complicación ficticia";

    private final AgendaEnMemoria agenda = new AgendaEnMemoria();

    @Test
    @DisplayName("Extensión sin conflictos")
    void extensionSinConflictos() {
        // GIVEN un turno activo de "Consulta" de 10:00 a 10:30 con la Dra. A en el Sillón 1 y margen de 10
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal extiende el turno con un fin absoluto de 10:45 y un motivo administrativo
        Turno extendido = extender(original, a(10, 45), MOTIVO);

        // THEN el turno pasa a terminar a las 10:45 con extendido verdadero
        assertThat(extendido.intervalo().fin()).isEqualTo(a(10, 45));
        assertThat(extendido.extendido()).isTrue();
        // AND conserva su numeroReserva original y sigue en estado RESERVADO
        assertThat(extendido.numeroReserva()).isEqualTo(original.numeroReserva());
        assertThat(extendido.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(extendido.id()).isEqualTo(original.id());
        assertThat(extendido.intervalo().inicio()).isEqualTo(a(10, 0));
        assertThat(agenda.turnos.porId(original.id()).intervalo().fin()).isEqualTo(a(10, 45));
    }

    @Test
    @DisplayName("Extensión que choca con el sillón")
    void extensionQueChocaConElSillon() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        agenda.existente(OTRO_PACIENTE, DR_B, SILLON_1, a(11, 0), EstadoTurno.RESERVADO);

        assertRechazo(() -> extender(original, a(11, 0), MOTIVO), CodigoRechazo.SOLAPAMIENTO_SILLON);

        Turno sinCambios = agenda.turnos.porId(original.id());
        assertThat(sinCambios.intervalo().fin()).isEqualTo(a(10, 30));
        assertThat(sinCambios.numeroReserva()).isEqualTo(original.numeroReserva());
    }

    @Test
    @DisplayName("Extensión sin margen suficiente")
    void extensionSinMargenSuficiente() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        agenda.existente(OTRO_PACIENTE, DR_B, SILLON_1, a(10, 45), EstadoTurno.RESERVADO);

        assertRechazo(() -> extender(original, a(10, 40), MOTIVO), CodigoRechazo.MARGEN_INSUFICIENTE);
    }

    @Test
    @DisplayName("Extensión de un sobreturno")
    void extensionDeUnSobreturno() {
        Turno sobreturno = agenda.turnos.guardar(Turno.sobreturno(PACIENTE_UNO, DRA_A, SILLON_1,
                AgendaEnMemoria.CONSULTA, new ar.turnos.odontologia.agenda.domain.Intervalo(a(10, 0), a(10, 30)),
                new NumeroReserva("K7M2-QX9P"), "Urgencia ficticia"));

        assertRechazo(() -> extender(sobreturno, a(10, 45), MOTIVO), CodigoRechazo.TURNO_NO_EXTENSIBLE);

        assertThat(agenda.turnos.porId(sobreturno.id()).intervalo().fin()).isEqualTo(a(10, 30));
    }

    @Test
    @DisplayName("Extensión de un turno inactivo")
    void extensionDeUnTurnoInactivo() {
        Turno cancelado = agenda.dePacienteUno(a(10, 0), EstadoTurno.CANCELADO);

        assertRechazo(() -> extender(cancelado, a(10, 45), MOTIVO), CodigoRechazo.TURNO_NO_ACTIVO);
    }

    @Test
    @DisplayName("Extensión con un fin no posterior")
    void extensionConUnFinNoPosterior() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        assertRechazo(() -> extender(original, a(10, 30), MOTIVO), CodigoRechazo.DATOS_INVALIDOS);
        assertRechazo(() -> extender(original, a(10, 20), MOTIVO), CodigoRechazo.DATOS_INVALIDOS);

        assertThat(agenda.turnos.porId(original.id()).intervalo().fin()).isEqualTo(a(10, 30));
    }

    @Test
    @DisplayName("Extensión que choca con la agenda del profesional")
    void extensionQueChocaConElProfesional() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        agenda.existente(OTRO_PACIENTE, DRA_A, SILLON_2, a(10, 45), EstadoTurno.RESERVADO);

        assertRechazo(() -> extender(original, a(10, 50), MOTIVO), CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
    }

    @Test
    @DisplayName("El margen de 15 minutos se aplica también a una extensión")
    void extensionConMargenDeQuinceMinutos() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        agenda.existente(OTRO_PACIENTE, DR_B, SILLON_1, a(10, 45), EstadoTurno.RESERVADO);
        ExtenderTurno caso = casoDeUso(Margen.deMinutos(15));

        assertThatThrownBy(() -> caso.ejecutar(comando(original, a(10, 35), MOTIVO)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.MARGEN_INSUFICIENTE);
    }

    @Test
    @DisplayName("Extensión de un turno inexistente")
    void extensionDeUnTurnoInexistente() {
        assertThatThrownBy(() -> casoDeUso(Margen.PREDETERMINADO)
                .ejecutar(new ExtenderTurnoCommand(999L, a(10, 45), MOTIVO)))
                .isInstanceOf(TurnoInexistenteException.class);
    }

    @Test
    @DisplayName("Extensión con motivo inválido")
    void extensionConMotivoInvalido() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        assertRechazo(() -> extender(original, a(10, 45), ""), CodigoRechazo.DATOS_INVALIDOS);
        assertRechazo(() -> extender(original, a(10, 45), "   "), CodigoRechazo.DATOS_INVALIDOS);
        assertRechazo(() -> extender(original, a(10, 45), "a".repeat(501)), CodigoRechazo.DATOS_INVALIDOS);
        assertThat(agenda.turnos.porId(original.id()).intervalo().fin()).isEqualTo(a(10, 30));
    }

    @Test
    @DisplayName("Extensión de un turno ya iniciado")
    void extensionDeUnTurnoYaIniciado() {
        // GIVEN un turno activo de 10:00 a 10:30 y el reloj en las 10:20
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);

        // WHEN el personal pide extenderlo con un fin absoluto de 10:45
        Turno extendido = casoDeUso(relojALas(10, 20), Margen.PREDETERMINADO)
                .ejecutar(comando(original, a(10, 45), MOTIVO));

        // THEN el turno pasa a terminar a las 10:45 con extendido verdadero
        assertThat(extendido.intervalo().fin()).isEqualTo(a(10, 45));
        assertThat(extendido.extendido()).isTrue();
        // AND conserva su numeroReserva original y su estado
        assertThat(extendido.numeroReserva()).isEqualTo(original.numeroReserva());
        assertThat(extendido.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(extendido.intervalo().inicio()).isEqualTo(a(10, 0));
    }

    @Test
    @DisplayName("Extensión de un turno ya iniciado con un fin que no está en el futuro")
    void extensionDeUnTurnoIniciadoConFinQueNoEstaEnElFuturo() {
        // GIVEN un turno activo de 10:00 a 10:30 y el reloj en las 10:50
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        ExtenderTurno caso = casoDeUso(relojALas(10, 50), Margen.PREDETERMINADO);

        // WHEN el personal pide extenderlo con un fin absoluto de 10:45
        // THEN la operación se rechaza con DATOS_INVALIDOS y el turno queda sin cambios
        assertRechazo(() -> caso.ejecutar(comando(original, a(10, 45), MOTIVO)), CodigoRechazo.DATOS_INVALIDOS);
        assertThat(agenda.turnos.porId(original.id()).intervalo().fin()).isEqualTo(a(10, 30));
    }

    @Test
    @DisplayName("Un turno ya iniciado se extiende respetando el margen del turno siguiente")
    void extensionDeUnTurnoIniciadoRespetaElMargen() {
        Turno original = agenda.dePacienteUno(a(10, 0), EstadoTurno.RESERVADO);
        agenda.existente(OTRO_PACIENTE, DR_B, SILLON_1, a(10, 45), EstadoTurno.RESERVADO);

        assertRechazo(() -> casoDeUso(relojALas(10, 20), Margen.PREDETERMINADO)
                .ejecutar(comando(original, a(10, 40), MOTIVO)), CodigoRechazo.MARGEN_INSUFICIENTE);
    }

    private ExtenderTurno casoDeUso(Margen margen) {
        return casoDeUso(relojALas(8, 0), margen);
    }

    private ExtenderTurno casoDeUso(java.time.Clock reloj, Margen margen) {
        CrearTurno crear = new CrearTurno(agenda.turnos, agenda.recursos, agenda.numeros, reloj, margen);
        return new ExtenderTurno(agenda.turnos, crear);
    }

    private Turno extender(Turno original, Instant fin, String motivo) {
        return casoDeUso(Margen.PREDETERMINADO).ejecutar(comando(original, fin, motivo));
    }

    private static ExtenderTurnoCommand comando(Turno original, Instant fin, String motivo) {
        return new ExtenderTurnoCommand(original.id(), fin, motivo);
    }

    private static void assertRechazo(ThrowingCallable accion, CodigoRechazo esperado) {
        assertThatThrownBy(accion)
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(esperado);
    }
}
