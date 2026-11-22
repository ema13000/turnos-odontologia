package ar.turnos.odontologia.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.agenda.application.CrearSobreturnoCommand;
import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** Red de seguridad de la base para sobreturnos: EXCLUDE, fin_con_margen y carrera por el último sillón. */
@Tag("integration")
@TestPropertySource(properties = "app.agenda.margen-minutos=15")
class SobreturnoInfraestructuraIntegrationTest extends IntegracionAgendaBase {

    private static final String MOTIVO = "Urgencia administrativa";

    @Autowired ServicioAgenda servicio;

    @Test
    @DisplayName("Sobreturno sin sillón libre")
    void sobreturnoSinSillonLibre() {
        sembrar(draA, sillon1, a(10, 0), a(10, 30));
        sembrar(drB, sillon2, a(10, 0), a(10, 30));

        assertRechazo(10, 35, CodigoRechazo.SIN_SILLON_DISPONIBLE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("El sobreturno guarda fin_con_margen y sus columnas")
    void guardaFinConMargen() {
        Turno turno = crear(10, 0);

        assertThat(turno.esSobreturno()).isTrue();
        assertThat(jdbc.queryForObject("SELECT es_sobreturno FROM turno WHERE id = ?", Boolean.class, turno.id()))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT motivo_sobreturno FROM turno WHERE id = ?", String.class, turno.id()))
                .isEqualTo(MOTIVO);
        assertThat(jdbc.queryForObject("SELECT fin - inicio FROM turno WHERE id = ?", String.class, turno.id()))
                .isEqualTo("00:30:00");
        assertThat(jdbc.queryForObject("SELECT fin_con_margen - fin FROM turno WHERE id = ?", String.class, turno.id()))
                .isEqualTo("00:15:00");
    }

    @Test
    @DisplayName("Sobreturnos simultáneos por el último sillón libre")
    void simultaneosPorUltimoSillon() throws Exception {
        sembrar(draA, sillon1, a(10, 0), a(10, 30));

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                resultados.add(hilos.submit(() -> {
                    salida.await();
                    try {
                        return crear(10, 10);
                    } catch (TurnoRechazadoException e) {
                        return e;
                    }
                }));
            }
            salida.countDown();

            List<Object> valores = new ArrayList<>();
            for (Future<Object> resultado : resultados) {
                valores.add(resultado.get());
            }
            assertThat(valores).anySatisfy(v -> assertThat(v).isInstanceOf(Turno.class));
            assertThat(valores).anySatisfy(v -> assertThat(v)
                    .isInstanceOfSatisfying(TurnoRechazadoException.class,
                            e -> assertThat(e.codigo()).isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE)));
        } finally {
            hilos.shutdownNow();
        }

        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno WHERE es_sobreturno", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("El margen de 15 minutos se aplica también en la infraestructura")
    void margenDeQuinceMinutos() {
        sembrar(draA, sillon1, a(10, 0), a(10, 30));
        sembrar(drB, sillon2, a(10, 0), a(10, 30));

        assertRechazo(10, 40, CodigoRechazo.SIN_SILLON_DISPONIBLE);

        jdbc.update("TRUNCATE turno RESTART IDENTITY CASCADE");
        Turno turno = crear(10, 45);
        assertThat(jdbc.queryForObject("SELECT fin_con_margen - fin FROM turno WHERE id = ?", String.class, turno.id()))
                .isEqualTo("00:15:00");
    }

    @Test
    @DisplayName("Un turno ya iniciado sembrado por SQL deja el sillón ocupado")
    void turnoYaIniciado() {
        sembrar(draA, sillon1, a(7, 50), a(8, 20));
        sembrar(drB, sillon2, a(7, 50), a(8, 20));

        assertRechazo(8, 0, CodigoRechazo.SIN_SILLON_DISPONIBLE);
    }

    private Turno crear(int hora, int minuto) {
        return servicio.crearSobreturno(comando(a(hora, minuto)), "recepcion");
    }

    private void assertRechazo(int hora, int minuto, CodigoRechazo esperado) {
        assertThatThrownBy(() -> crear(hora, minuto))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(esperado);
    }

    private CrearSobreturnoCommand comando(Instant inicio) {
        return new CrearSobreturnoCommand(pacienteUno, draA, consulta, inicio, MOTIVO);
    }

    private void sembrar(long profesional, long sillon, Instant inicio, Instant fin) {
        jdbc.update("INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, inicio, fin, "
                        + "fin_con_margen, estado, es_sobreturno) VALUES (?,?,?,?,?,?,?, 'RESERVADO', false)",
                profesional, sillon, consulta, pacienteUno,
                inicio.atOffset(ZoneOffset.UTC), fin.atOffset(ZoneOffset.UTC),
                fin.plusSeconds(10 * 60).atOffset(ZoneOffset.UTC));
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
