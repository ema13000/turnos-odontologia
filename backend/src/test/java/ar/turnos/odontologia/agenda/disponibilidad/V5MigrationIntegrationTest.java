package ar.turnos.odontologia.agenda.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Esquema de V5 contra PostgreSQL real: la base también impide franjas superpuestas (design D7). */
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class V5MigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long profesional;

    @BeforeEach
    void profesionalFicticio() {
        jdbc.execute("TRUNCATE usuario, turno, paciente, prestacion, sillon, profesional RESTART IDENTITY CASCADE");
        profesional = jdbc.queryForObject(
                "INSERT INTO profesional(nombre, apellido, matricula) VALUES ('Ana','Ejemplo','MP-0001') RETURNING id",
                Long.class);
    }

    private void franja(int dia, String desde, String hasta) {
        jdbc.update("INSERT INTO horario_trabajo(profesional_id, dia_semana, hora_desde, hora_hasta) "
                + "VALUES (?, ?, ?::time, ?::time)", profesional, dia, desde, hasta);
    }

    @Test
    @DisplayName("La base rechaza dos franjas superpuestas del mismo profesional y día")
    void rechazaFranjasSuperpuestas() {
        franja(2, "08:00", "12:00");

        assertThatThrownBy(() -> franja(2, "11:00", "14:00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("horario_trabajo_sin_superposicion");
    }

    @Test
    @DisplayName("La base acepta dos franjas contiguas y la misma franja en otro día")
    void aceptaFranjasContiguas() {
        franja(2, "08:00", "12:00");
        franja(2, "12:00", "14:00");
        franja(3, "08:00", "12:00");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM horario_trabajo", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("La base rechaza una franja con el fin antes del inicio y un día fuera de 1 a 7")
    void rechazaFranjasInvalidas() {
        assertThatThrownBy(() -> franja(2, "12:00", "08:00"))
                .hasMessageContaining("horario_trabajo_rango_valido");
        assertThatThrownBy(() -> franja(8, "08:00", "12:00"))
                .hasMessageContaining("horario_trabajo_dia_valido");
    }

    @Test
    @DisplayName("La base rechaza ausencias y bloqueos con el fin antes del inicio")
    void rechazaPeriodosInvalidos() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ausencia_profesional(profesional_id, tipo, inicio, fin, "
                + "registrado_por, registrado_en) VALUES (?, 'AUSENCIA', now(), now() - interval '1 hour', 'admin', now())",
                profesional))
                .hasMessageContaining("ausencia_profesional_periodo_valido");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO bloqueo_agenda(profesional_id, inicio, fin, registrado_por, "
                + "registrado_en) VALUES (?, now(), now() - interval '1 hour', 'admin', now())", profesional))
                .hasMessageContaining("bloqueo_agenda_periodo_valido");
    }
}
