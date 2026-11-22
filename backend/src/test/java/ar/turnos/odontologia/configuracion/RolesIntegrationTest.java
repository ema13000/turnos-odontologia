package ar.turnos.odontologia.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Permisos por rol de la configuración: el personal consulta y solo ADMIN modifica (matriz RBAC
 * de la KB y requisito "Permisos por rol en cada operación del personal" de C-05). Los datos
 * previos se cargan por JDBC porque la sesión de cada test no es de ADMIN.
 */
class RolesIntegrationTest extends ConfiguracionIntegrationTest {

    // ---------- Requirement: Solo el administrador modifica el catálogo ----------

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    @DisplayName("Recepción consulta los profesionales")
    void recepcionConsultaLosProfesionales() throws Exception {
        // GIVEN la Dra. Ana Ejemplo dada de alta
        jdbc.update("INSERT INTO profesional (nombre, apellido, matricula) VALUES ('Ana', 'Ejemplo', 'MP-0001')");

        // AND el usuario recepcion con sesión iniciada; WHEN consulta los profesionales
        obtener("/profesionales")
                // THEN la respuesta es 200 e incluye a la Dra. Ana Ejemplo
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matricula", hasItem("MP-0001")));
    }

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    @DisplayName("Recepción intenta dar de alta un profesional")
    void recepcionIntentaDarDeAltaUnProfesional() throws Exception {
        // GIVEN el usuario recepcion con sesión iniciada
        // WHEN da de alta a Ana Ejemplo con la matrícula MP-0001
        postJson("/profesionales", """
                {"nombre":"Ana","apellido":"Ejemplo","matricula":"MP-0001"}""")
                // THEN la respuesta es 403 con ACCESO_DENEGADO
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        // AND no se crea ningún profesional
        assertThat(jdbc.queryForObject("SELECT count(*) FROM profesional", Integer.class)).isZero();
    }

    @Test
    @WithMockUser(username = "ana.ejemplo", roles = "ODONTOLOGO")
    @DisplayName("Odontólogo intenta desactivar un sillón")
    void odontologoIntentaDesactivarUnSillon() throws Exception {
        // GIVEN el "Sillón 1" activo
        Long sillon1 = jdbc.queryForObject("INSERT INTO sillon (nombre) VALUES ('Sillón 1') RETURNING id", Long.class);

        // AND el usuario ana.ejemplo (ODONTOLOGO) con sesión iniciada; WHEN desactiva el "Sillón 1"
        postVacio("/sillones/" + sillon1 + "/desactivar")
                // THEN la respuesta es 403 con ACCESO_DENEGADO
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        // AND el "Sillón 1" sigue activo
        assertThat(jdbc.queryForObject("SELECT activo FROM sillon WHERE id = ?", Boolean.class, sillon1)).isTrue();
    }

    // ---------- Requirement: Solo el administrador cambia el margen ----------

    @Test
    @WithMockUser(username = "recepcion", roles = "RECEPCIONISTA")
    @DisplayName("Recepción consulta el margen")
    void recepcionConsultaElMargen() throws Exception {
        // GIVEN un margen de 10 minutos, AND el usuario recepcion con sesión iniciada
        // WHEN consulta el margen
        obtener("/configuracion/margen")
                // THEN la respuesta es 200 con 10 minutos
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutos").value(10));
    }

    @Test
    @WithMockUser(username = "ana.ejemplo", roles = "ODONTOLOGO")
    @DisplayName("Odontólogo intenta cambiar el margen")
    void odontologoIntentaCambiarElMargen() throws Exception {
        // GIVEN un margen de 10 minutos, AND el usuario ana.ejemplo (ODONTOLOGO) con sesión iniciada
        // WHEN cambia el margen a 20 minutos
        putJson("/configuracion/margen", """
                {"minutos":20}""")
                // THEN la respuesta es 403 con ACCESO_DENEGADO
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        // AND el margen vigente sigue siendo de 10 minutos
        assertThat(jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = 'margen_minutos'", String.class))
                .isEqualTo("10");
    }
}
