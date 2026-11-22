package ar.turnos.odontologia.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Extensión de un turno existente sin cambiar su identidad (RN-AG-14). */
class TurnoExtensionTest {

    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    @DisplayName("Extensión sin conflictos")
    void extensionSinConflictos() {
        NumeroReserva numero = new NumeroReserva("K7M2-QX9P");
        Turno original = new Turno(9L, 1L, 1L, 1L, 1L,
                new Intervalo(a(10, 0), a(10, 30)), EstadoTurno.RESERVADO, numero);

        Turno extendido = original.conExtension(a(10, 45), "Complicación ficticia");

        assertThat(extendido.intervalo().inicio()).isEqualTo(a(10, 0));
        assertThat(extendido.intervalo().fin()).isEqualTo(a(10, 45));
        assertThat(extendido.extendido()).isTrue();
        assertThat(extendido.motivoExtension()).isEqualTo("Complicación ficticia");
        assertThat(extendido.numeroReserva()).isSameAs(numero);
        assertThat(extendido.id()).isEqualTo(9L);
        assertThat(extendido.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(original.intervalo().fin()).isEqualTo(a(10, 30));
    }

    @Test
    @DisplayName("Extensión con un fin no posterior")
    void extensionConFinNoPosterior() {
        Turno original = new Turno(9L, 1L, 1L, 1L, 1L,
                new Intervalo(a(10, 0), a(10, 30)), EstadoTurno.RESERVADO, new NumeroReserva("K7M2-QX9P"));

        assertThatThrownBy(() -> original.conExtension(a(10, 30), "Complicación ficticia"))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.DATOS_INVALIDOS);
        assertThatThrownBy(() -> original.conExtension(a(10, 20), "Complicación ficticia"))
                .isInstanceOf(TurnoRechazadoException.class);
    }

    @Test
    @DisplayName("El motivo de la extensión admite 500 caracteres pero no 501 ni espacios")
    void motivoDeLaExtensionValidaLongitud() {
        Turno original = new Turno(9L, 1L, 1L, 1L, 1L,
                new Intervalo(a(10, 0), a(10, 30)), EstadoTurno.RESERVADO, new NumeroReserva("K7M2-QX9P"));

        assertThat(original.conExtension(a(10, 45), "a".repeat(500)).motivoExtension()).hasSize(500);
        assertThatThrownBy(() -> original.conExtension(a(10, 45), "a".repeat(501)))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.DATOS_INVALIDOS);
        assertThatThrownBy(() -> original.conExtension(a(10, 45), "   "))
                .isInstanceOf(TurnoRechazadoException.class);
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
