package ar.turnos.odontologia.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El sobreturno queda marcado y lleva su motivo administrativo (RN-AG-06). */
class TurnoSobreturnoTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    @DisplayName("Sobreturno con motivo válido")
    void sobreturnoConMotivoValido() {
        // GIVEN una agenda sin otros turnos y un motivo administrativo
        NumeroReserva numero = new NumeroReserva("K7M2-QX9P");

        // WHEN se crea el sobreturno de "Consulta" con la Dra. A a las 10:00
        Turno sobreturno = Turno.sobreturno(1L, 1L, 1L, 1L,
                new Intervalo(a(10, 0), a(10, 30)), numero, "Urgencia con el Paciente Uno");

        // THEN queda en RESERVADO, marcado como sobreturno, con su motivo y sin exponerlo en la respuesta
        assertThat(sobreturno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(sobreturno.esSobreturno()).isTrue();
        assertThat(sobreturno.motivoSobreturno()).isEqualTo("Urgencia con el Paciente Uno");
        assertThat(sobreturno.numeroReserva()).isSameAs(numero);
        assertThat(sobreturno.extendido()).isFalse();
    }

    @Test
    @DisplayName("El motivo del sobreturno admite 500 caracteres pero no 501 ni espacios")
    void motivoDelSobreturnoValidaLongitud() {
        Intervalo intervalo = new Intervalo(a(10, 0), a(10, 30));
        NumeroReserva numero = new NumeroReserva("K7M2-QX9P");

        assertThat(Turno.sobreturno(1L, 1L, 1L, 1L, intervalo, numero, "a".repeat(500))
                .motivoSobreturno()).hasSize(500);
        assertThatThrownBy(
                () -> Turno.sobreturno(1L, 1L, 1L, 1L, intervalo, numero, "a".repeat(501)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.DATOS_INVALIDOS);
        assertThatThrownBy(
                () -> Turno.sobreturno(1L, 1L, 1L, 1L, intervalo, numero, "   "))
                .isInstanceOf(TurnoRechazadoException.class);
    }

    @Test
    @DisplayName("conId y cancelado conservan la marca de sobreturno y su motivo")
    void identidadYEstadoConservanLaMarca() {
        Turno sobreturno = Turno.sobreturno(1L, 1L, 1L, 1L,
                new Intervalo(a(10, 0), a(10, 30)), new NumeroReserva("K7M2-QX9P"), "Urgencia ficticia");

        Turno guardado = sobreturno.conId(7L).cancelado();

        assertThat(guardado.id()).isEqualTo(7L);
        assertThat(guardado.estado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(guardado.esSobreturno()).isTrue();
        assertThat(guardado.motivoSobreturno()).isEqualTo("Urgencia ficticia");
        assertThat(guardado.numeroReserva()).isSameAs(sobreturno.numeroReserva());
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
