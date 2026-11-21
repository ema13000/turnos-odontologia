package ar.turnos.odontologia.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.agenda.application.TurnoRepository;
import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.NumeroReserva;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.LocalDateTime;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Garantía de la base (design D6): guardar un turno sin pasar por el dominio y que pise a otro hace saltar el
 * EXCLUDE, y el adaptador lo traduce al mismo código de rechazo de la agenda.
 */
@Tag("integration")
class TurnoRepositoryJpaIntegrationTest extends IntegracionAgendaBase {

    @Autowired TurnoRepository turnos;
    @Autowired TransactionTemplate transaccion;

    @Test
    void elExcludeDelSillonSeTraduceASolapamientoSillon() {
        guardar(draA, sillon1, a(10, 0), "AAAA-AAAA");

        assertThatThrownBy(() -> guardar(drB, sillon1, a(10, 15), "BBBB-BBBB"))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SOLAPAMIENTO_SILLON);
    }

    @Test
    void elExcludeDelProfesionalSeTraduceASolapamientoProfesional() {
        guardar(draA, sillon1, a(10, 0), "AAAA-AAAA");

        assertThatThrownBy(() -> guardar(draA, sillon2, a(10, 15), "BBBB-BBBB"))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SOLAPAMIENTO_PROFESIONAL);
    }

    @Test
    void guardaYLeeElTurnoConSuNumeroDeReserva() {
        Turno guardado = guardar(draA, sillon1, a(10, 0), "K7M2-QX9P");

        Turno leido = transaccion.execute(s -> turnos.buscarParaModificar(guardado.id()).orElseThrow().turno());
        assertThat(leido.numeroReserva().valor()).isEqualTo("K7M2-QX9P");
        assertThat(leido.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(jdbc.queryForObject("SELECT fin_con_margen - fin FROM turno WHERE id = ?", String.class, guardado.id()))
                .isEqualTo("00:10:00");
    }

    private Turno guardar(long profesional, long sillon, Instant inicio, String numero) {
        Turno turno = new Turno(null, pacienteUno, profesional, sillon, consulta,
                new Intervalo(inicio, inicio.plusSeconds(30 * 60)), EstadoTurno.RESERVADO, new NumeroReserva(numero));
        return transaccion.execute(s -> turnos.guardar(turno));
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
