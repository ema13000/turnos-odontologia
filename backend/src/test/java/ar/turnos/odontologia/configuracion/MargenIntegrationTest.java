package ar.turnos.odontologia.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.turnos.odontologia.TurnosOdontologiaApplication;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.configuracion.application.MargenVigente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Escenarios del spec {@code configuracion-margen}. */
class MargenIntegrationTest extends ConfiguracionIntegrationTest {

    // ---------- Requirement: Margen predeterminado ----------

    @Test
    @DisplayName("Consultar el margen sin haberlo cambiado")
    void consultarElMargenSinHaberloCambiado() throws Exception {
        // GIVEN un consultorio recién instalado
        // WHEN el administrador consulta el margen
        obtener("/configuracion/margen")
                // THEN el margen es de 10 minutos
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutos").value(10));
    }

    // ---------- Requirement: Cambiar el margen con un mínimo de 10 minutos ----------

    @Test
    @DisplayName("Aumentar el margen")
    void aumentarElMargen() throws Exception {
        // GIVEN un margen de 10 minutos
        // WHEN el administrador cambia el margen a 15 minutos
        putJson("/configuracion/margen", """
                {"minutos":15}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutos").value(15));

        // THEN el margen vigente es de 15 minutos
        obtener("/configuracion/margen").andExpect(jsonPath("$.minutos").value(15));
    }

    @Test
    @DisplayName("Margen menor al mínimo")
    void margenMenorAlMinimo() throws Exception {
        // GIVEN un margen de 15 minutos
        putJson("/configuracion/margen", """
                {"minutos":15}""").andExpect(status().isOk());

        // WHEN el administrador cambia el margen a 8 minutos
        putJson("/configuracion/margen", """
                {"minutos":8}""")
                // THEN el cambio se rechaza con 422 y MARGEN_INVALIDO
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MARGEN_INVALIDO"));

        // AND el margen vigente sigue siendo de 15 minutos
        obtener("/configuracion/margen").andExpect(jsonPath("$.minutos").value(15));
    }

    @Test
    @DisplayName("Margen igual al mínimo")
    void margenIgualAlMinimo() throws Exception {
        // GIVEN un margen de 15 minutos
        putJson("/configuracion/margen", """
                {"minutos":15}""").andExpect(status().isOk());

        // WHEN el administrador cambia el margen a 10 minutos
        putJson("/configuracion/margen", """
                {"minutos":10}""")
                .andExpect(status().isOk());

        // THEN el margen vigente es de 10 minutos
        obtener("/configuracion/margen").andExpect(jsonPath("$.minutos").value(10));
    }

    // ---------- Requirement: El margen vigente se conserva ----------

    @Test
    @DisplayName("Margen después de reiniciar")
    void margenDespuesDeReiniciar() throws Exception {
        // GIVEN que el administrador cambió el margen a 15 minutos
        putJson("/configuracion/margen", """
                {"minutos":15}""").andExpect(status().isOk());

        // WHEN la aplicación se reinicia (un contexto de Spring nuevo). Desde C-05 la seguridad
        // necesita el contexto web, así que arranca como servlet en un puerto libre.
        try (ConfigurableApplicationContext reiniciada = new SpringApplicationBuilder(TurnosOdontologiaApplication.class)
                .profiles("test")
                .web(WebApplicationType.SERVLET)
                .properties("server.port=0")
                .run()) {

            // THEN el margen vigente sigue siendo de 15 minutos
            Margen vigente = reiniciada.getBean(MargenVigente.class).actual();
            assertThat(vigente).isEqualTo(Margen.deMinutos(15));
        }
    }
}
