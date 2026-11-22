package ar.turnos.odontologia.agenda.infrastructure;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.agenda.application.TurnoRepository;
import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.NumeroReserva;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.shared.persistence.TurnoEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Persistencia de la extensión en la misma fila del turno (design D5). */
class TurnoEntityExtensionIntegrationTest extends IntegracionAgendaBase {

    @Autowired TurnoRepository turnos;
    @Autowired TransactionTemplate transaccion;
    @Autowired EntityManager em;

    @Test
    @DisplayName("guarda extendido, motivoExtension, fin y finConMargen")
    void guardaExtension() {
        Turno inicial = guardar();
        Instant fin = a(10, 45);
        Instant finConMargen = a(10, 55);

        actualizar(inicial.id(), fin, finConMargen, "Complicación ficticia");

        assertThat(jdbc.queryForObject("SELECT extendido FROM turno WHERE id = ?", Boolean.class, inicial.id())).isTrue();
        assertThat(jdbc.queryForObject("SELECT motivo_extension FROM turno WHERE id = ?", String.class, inicial.id()))
                .isEqualTo("Complicación ficticia");
        assertThat(jdbc.queryForObject("SELECT fin FROM turno WHERE id = ?", Instant.class, inicial.id())).isEqualTo(fin);
        assertThat(jdbc.queryForObject("SELECT fin_con_margen FROM turno WHERE id = ?", Instant.class, inicial.id()))
                .isEqualTo(finConMargen);
        assertThat(jdbc.queryForObject("SELECT version FROM turno WHERE id = ?", Long.class, inicial.id()))
                .isGreaterThan(0L);
    }

    @Test
    @DisplayName("extender dos veces el mismo turno deja el último fin y motivo")
    void extenderDosVecesDejaElUltimoValor() {
        Turno inicial = guardar();

        actualizar(inicial.id(), a(10, 45), a(10, 55), "Primera extensión ficticia");
        long versionTrasPrimera = jdbc.queryForObject("SELECT version FROM turno WHERE id = ?", Long.class, inicial.id());
        actualizar(inicial.id(), a(11, 0), a(11, 10), "Segunda extensión ficticia");

        assertThat(jdbc.queryForObject("SELECT fin FROM turno WHERE id = ?", Instant.class, inicial.id())).isEqualTo(a(11, 0));
        assertThat(jdbc.queryForObject("SELECT fin_con_margen FROM turno WHERE id = ?", Instant.class, inicial.id()))
                .isEqualTo(a(11, 10));
        assertThat(jdbc.queryForObject("SELECT motivo_extension FROM turno WHERE id = ?", String.class, inicial.id()))
                .isEqualTo("Segunda extensión ficticia");
        assertThat(jdbc.queryForObject("SELECT version FROM turno WHERE id = ?", Long.class, inicial.id()))
                .isGreaterThan(versionTrasPrimera);
        assertThat(jdbc.queryForObject("SELECT codigo_reserva FROM turno WHERE id = ?", String.class, inicial.id()))
                .isEqualTo("K7M2-QX9P");
    }

    private Turno guardar() {
        Instant inicio = a(10, 0);
        Turno turno = new Turno(null, pacienteUno, draA, sillon1, consulta,
                new Intervalo(inicio, a(10, 30)), EstadoTurno.RESERVADO, new NumeroReserva("K7M2-QX9P"));
        return transaccion.execute(s -> turnos.guardar(turno));
    }

    private void actualizar(long id, Instant fin, Instant finConMargen, String motivo) {
        transaccion.execute(s -> {
            TurnoEntity entidad = em.find(TurnoEntity.class, id);
            entidad.actualizarExtension(fin, finConMargen, motivo);
            return null;
        });
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
