package ar.turnos.odontologia.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Escenarios de sillones y prestaciones del spec {@code catalogo-consultorio}. */
class SillonesYPrestacionesIntegrationTest extends ConfiguracionIntegrationTest {

    // ---------- Requirement: Sillones con nombre único ----------

    @Test
    @DisplayName("Nombre de sillón duplicado")
    void nombreDeSillonDuplicado() throws Exception {
        // GIVEN un sillón llamado "Sillón 1"
        crearSillon("Sillón 1");

        // WHEN el administrador da de alta otro sillón llamado "Sillón 1"
        postJson("/sillones", """
                {"nombre":"Sillón 1"}""")
                // THEN el alta se rechaza con 409 y NOMBRE_DUPLICADO
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_DUPLICADO"));
    }

    // ---------- Requirement: Desactivar en lugar de borrar ----------

    @Test
    @DisplayName("Reactivar un sillón")
    void reactivarUnSillon() throws Exception {
        // GIVEN el "Sillón 2" desactivado
        long sillon2 = crearSillon("Sillón 2");
        postVacio("/sillones/" + sillon2 + "/desactivar").andExpect(status().isOk());

        // WHEN el administrador lo reactiva
        postVacio("/sillones/" + sillon2 + "/activar").andExpect(status().isOk());

        // THEN el "Sillón 2" figura como activo
        obtener("/sillones/" + sillon2)
                .andExpect(jsonPath("$.activo").value(true));
    }

    // ---------- Requirement: Prestaciones con nombre único y duración válida ----------

    @Test
    @DisplayName("Prestación con duración válida")
    void prestacionConDuracionValida() throws Exception {
        // WHEN el administrador da de alta "Limpieza (profilaxis)" con 45 minutos
        postJson("/prestaciones", """
                {"nombre":"Limpieza (profilaxis)","duracionMinutos":45}""")
                // THEN la prestación queda creada y activa, con 45 minutos
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Limpieza (profilaxis)"))
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("Duración que no es múltiplo de 5")
    void duracionQueNoEsMultiploDe5() throws Exception {
        // WHEN el administrador da de alta una prestación de 32 minutos
        postJson("/prestaciones", """
                {"nombre":"Control rápido","duracionMinutos":32}""")
                // THEN el alta se rechaza con 422 y DURACION_INVALIDA
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DURACION_INVALIDA"));
    }

    @Test
    @DisplayName("Duración cero")
    void duracionCero() throws Exception {
        // WHEN el administrador da de alta una prestación de 0 minutos
        postJson("/prestaciones", """
                {"nombre":"Control rápido","duracionMinutos":0}""")
                // THEN el alta se rechaza con 422 y DURACION_INVALIDA
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DURACION_INVALIDA"));
    }

    // ---------- Requirement: Cambiar la duración no modifica los turnos creados ----------

    @Test
    @DisplayName("Turno existente después de cambiar la duración")
    void turnoExistenteDespuesDeCambiarLaDuracion() throws Exception {
        // GIVEN un turno de "Consulta o control" de 10:00 a 10:30
        long consulta = crearPrestacion("Consulta o control", 30);
        long ana = crearProfesional("Ana", "Ejemplo", "MP-0001");
        long sillon1 = crearSillon("Sillón 1");
        Long paciente = jdbc.queryForObject(
                "INSERT INTO paciente (nombre, apellido, dni) VALUES ('Paciente', 'Ficticio', '99000001') RETURNING id",
                Long.class);
        OffsetDateTime inicio = OffsetDateTime.of(2030, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-3));
        OffsetDateTime fin = inicio.plusMinutes(30);
        Long turno = jdbc.queryForObject("""
                INSERT INTO turno (profesional_id, sillon_id, prestacion_id, paciente_id, inicio, fin, fin_con_margen)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                ana, sillon1, consulta, paciente, inicio, fin, fin.plusMinutes(10));

        // WHEN el administrador cambia la duración de "Consulta o control" a 40 minutos
        putJson("/prestaciones/" + consulta, """
                {"nombre":"Consulta o control","duracionMinutos":40}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionMinutos").value(40));

        // THEN el turno existente sigue terminando a las 10:30
        Timestamp finGuardado = jdbc.queryForObject("SELECT fin FROM turno WHERE id = ?", Timestamp.class, turno);
        assertThat(finGuardado.toInstant()).isEqualTo(fin.toInstant());
    }
}
