package ar.turnos.odontologia.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.IntegracionAgendaBase;
import ar.turnos.odontologia.agenda.application.CrearSobreturnoCommand;
import ar.turnos.odontologia.agenda.application.CrearTurnoCommand;
import ar.turnos.odontologia.agenda.application.ExtenderTurnoCommand;
import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Un test por escenario del spec {@code auditoria-turnos} para las acciones nuevas de C-08. */
@Tag("integration")
class AuditoriaSobreturnoExtensionIntegrationTest extends IntegracionAgendaBase {

    private static final String MOTIVO_SOBRETURNO = "Urgencia administrativa ficticia";
    private static final String MOTIVO_EXTENSION = "Complicación administrativa ficticia";

    @Autowired ServicioAgenda servicio;

    @Test
    @DisplayName("Se registra el sobreturno")
    void seRegistraElSobreturno() {
        Turno turno = crearSobreturno(a(10, 0), MOTIVO_SOBRETURNO, "recepcion");

        assertThat(registros(turno.id())).containsExactly(Map.of("accion", "SOBRETURNO", "actor", "recepcion"));
        assertThat(jdbc.queryForObject("SELECT fecha IS NOT NULL FROM turno_auditoria WHERE turno_id = ?",
                Boolean.class, turno.id())).isTrue();
    }

    @Test
    @DisplayName("Se registra la extensión")
    void seRegistraLaExtension() {
        Turno original = crearTurno("ana.ejemplo");

        Turno extendido = servicio.extender(new ExtenderTurnoCommand(original.id(), a(10, 45), MOTIVO_EXTENSION),
                "ana.ejemplo");

        assertThat(extendido.id()).isEqualTo(original.id());
        assertThat(registros(original.id())).contains(Map.of("accion", "EXTENDER", "actor", "ana.ejemplo"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT extendido FROM turno WHERE id = ?", Boolean.class, original.id()))
                .isTrue();
    }

    @Test
    @DisplayName("Sobreturno rechazado sin registro")
    void sobreturnoRechazadoSinRegistro() {
        sembrar(draA, sillon1, a(10, 0), a(10, 30));
        sembrar(drB, sillon2, a(10, 0), a(10, 30));

        assertThatThrownBy(() -> crearSobreturno(a(10, 10), MOTIVO_SOBRETURNO, "recepcion"))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SIN_SILLON_DISPONIBLE);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE accion = 'SOBRETURNO'",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("Extensión rechazada sin registro")
    void extensionRechazadaSinRegistro() {
        Turno original = crearTurno("recepcion");
        sembrar(drB, sillon1, a(11, 0), a(11, 30));

        assertThatThrownBy(() -> servicio.extender(
                new ExtenderTurnoCommand(original.id(), a(11, 0), MOTIVO_EXTENSION), "recepcion"))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.SOLAPAMIENTO_SILLON);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM turno_auditoria WHERE accion = 'EXTENDER'",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT fin FROM turno WHERE id = ?", Instant.class, original.id()))
                .isEqualTo(a(10, 30));
    }

    @Test
    @DisplayName("El motivo del sobreturno no se audita")
    void elMotivoDelSobreturnoNoSeAudita() {
        Turno turno = crearSobreturno(a(10, 0), MOTIVO_SOBRETURNO, "recepcion");

        assertThat(registroComoJson(turno.id(), "SOBRETURNO")).isNotBlank().doesNotContain(MOTIVO_SOBRETURNO);
    }

    @Test
    @DisplayName("El motivo de la extensión no se audita")
    void elMotivoDeLaExtensionNoSeAudita() {
        Turno original = crearTurno("ana.ejemplo");

        servicio.extender(new ExtenderTurnoCommand(original.id(), a(10, 45), MOTIVO_EXTENSION), "ana.ejemplo");

        assertThat(registroComoJson(original.id(), "EXTENDER")).isNotBlank().doesNotContain(MOTIVO_EXTENSION);
    }

    private Turno crearTurno(String actor) {
        return servicio.crear(new CrearTurnoCommand(pacienteUno, draA, sillon1, consulta, a(10, 0)), actor);
    }

    private Turno crearSobreturno(Instant inicio, String motivo, String actor) {
        return servicio.crearSobreturno(new CrearSobreturnoCommand(pacienteUno, draA, consulta, inicio, motivo), actor);
    }

    private void sembrar(long profesional, long sillon, Instant inicio, Instant fin) {
        jdbc.update("INSERT INTO turno(profesional_id, sillon_id, prestacion_id, paciente_id, inicio, fin, "
                        + "fin_con_margen, estado, es_sobreturno) VALUES (?,?,?,?,?,?,?, 'RESERVADO', false)",
                profesional, sillon, consulta, pacienteUno, inicio.atOffset(ZoneOffset.UTC),
                fin.atOffset(ZoneOffset.UTC), fin.plusSeconds(10 * 60).atOffset(ZoneOffset.UTC));
    }

    private List<Map<String, Object>> registros(long turnoId) {
        return jdbc.queryForList("SELECT accion, actor FROM turno_auditoria WHERE turno_id = ? ORDER BY id", turnoId);
    }

    private String registroComoJson(long turnoId, String accion) {
        return jdbc.queryForObject("SELECT row_to_json(a)::text FROM turno_auditoria a WHERE turno_id = ? "
                + "AND accion = ?", String.class, turnoId, accion);
    }

    private static Instant a(int hora, int minuto) {
        return LocalDateTime.of(2026, 12, 1, hora, minuto).atZone(BUENOS_AIRES).toInstant();
    }
}
