package ar.turnos.odontologia.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Confirmación en el dominio (C-11, spec {@code agenda-turnos}): solo un turno RESERVADO pasa a CONFIRMADO. */
class TurnoConfirmacionTest {

    private static final Instant DIEZ = Instant.parse("2026-12-01T13:00:00Z");

    private static Turno turno(EstadoTurno estado) {
        return new Turno(5L, 1L, 2L, 3L, 4L, new Intervalo(DIEZ, DIEZ.plusSeconds(30 * 60)), estado,
                new NumeroReserva("ABCD-2345"));
    }

    @Test
    void confirmarUnTurnoReservadoSoloCambiaElEstado() {
        Turno reservado = turno(EstadoTurno.RESERVADO);

        Turno confirmado = reservado.confirmado();

        assertThat(confirmado.estado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(confirmado).usingRecursiveComparison().ignoringFields("estado").isEqualTo(reservado);
        // el original no cambia
        assertThat(reservado.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    void elTurnoConfirmadoSigueOcupandoLaAgenda() {
        assertThat(turno(EstadoTurno.RESERVADO).confirmado().esActivo()).isTrue();
    }

    @Test
    void soloUnTurnoReservadoSePuedeConfirmar() {
        for (EstadoTurno estado : EstadoTurno.values()) {
            if (estado == EstadoTurno.RESERVADO) {
                continue;
            }
            assertThatThrownBy(() -> turno(estado).confirmado())
                    .as("confirmar un turno %s", estado)
                    .isInstanceOf(TurnoRechazadoException.class)
                    .extracting(e -> ((TurnoRechazadoException) e).codigo())
                    .isEqualTo(CodigoRechazo.TURNO_NO_ACTIVO);
        }
    }
}
