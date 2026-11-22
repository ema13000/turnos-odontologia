package ar.turnos.odontologia.configuracion;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Escenarios de profesionales del spec {@code catalogo-consultorio}. */
class ProfesionalesIntegrationTest extends ConfiguracionIntegrationTest {

    // ---------- Requirement: Alta de profesionales con matrícula única ----------

    @Test
    @DisplayName("Alta de un profesional")
    void altaDeUnProfesional() throws Exception {
        // GIVEN que no hay ningún profesional con la matrícula MP-0001
        // WHEN el administrador da de alta a Ana Ejemplo con la matrícula MP-0001
        postJson("/profesionales", """
                {"nombre":"Ana","apellido":"Ejemplo","matricula":"MP-0001"}""")
                // THEN el profesional queda creado y activo, AND la respuesta incluye sus datos
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Ejemplo"))
                .andExpect(jsonPath("$.matricula").value("MP-0001"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("Matrícula duplicada")
    void matriculaDuplicada() throws Exception {
        // GIVEN la Dra. Ana Ejemplo con la matrícula MP-0001
        crearProfesional("Ana", "Ejemplo", "MP-0001");

        // WHEN el administrador da de alta a otro profesional con la matrícula MP-0001
        postJson("/profesionales", """
                {"nombre":"Bruno","apellido":"Prueba","matricula":"MP-0001"}""")
                // THEN el alta se rechaza con 409 y MATRICULA_DUPLICADA
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATRICULA_DUPLICADA"));
    }

    // ---------- Requirement: Modificación de profesionales ----------

    @Test
    @DisplayName("Cambio de matrícula a una que ya existe")
    void cambioDeMatriculaAUnaQueYaExiste() throws Exception {
        // GIVEN la Dra. Ana Ejemplo con MP-0001 y el Dr. Bruno Prueba con MP-0002
        crearProfesional("Ana", "Ejemplo", "MP-0001");
        long bruno = crearProfesional("Bruno", "Prueba", "MP-0002");

        // WHEN el administrador cambia la matrícula del Dr. Bruno Prueba a MP-0001
        putJson("/profesionales/" + bruno, """
                {"nombre":"Bruno","apellido":"Prueba","matricula":"MP-0001"}""")
                // THEN el cambio se rechaza con 409 y MATRICULA_DUPLICADA
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATRICULA_DUPLICADA"));
    }

    // ---------- Requirement: Desactivar en lugar de borrar ----------

    @Test
    @DisplayName("Desactivar un profesional")
    void desactivarUnProfesional() throws Exception {
        // GIVEN la Dra. Ana Ejemplo activa
        long ana = crearProfesional("Ana", "Ejemplo", "MP-0001");

        // WHEN el administrador la desactiva
        postVacio("/profesionales/" + ana + "/desactivar").andExpect(status().isOk());

        // THEN figura como inactiva al consultarla, AND sus datos siguen disponibles
        obtener("/profesionales/" + ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.matricula").value("MP-0001"));
        obtener("/profesionales")
                .andExpect(jsonPath("$[*].matricula", hasItem("MP-0001")));
    }

    // ---------- Requirement: Recurso inexistente ----------

    @Test
    @DisplayName("Profesional inexistente")
    void profesionalInexistente() throws Exception {
        // WHEN el administrador consulta un profesional con un id que no existe
        obtener("/profesionales/999")
                // THEN la respuesta es 404 con RECURSO_NO_ENCONTRADO
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    // ---------- Requirement: Datos de entrada obligatorios ----------

    @Test
    @DisplayName("Profesional sin apellido")
    void profesionalSinApellido() throws Exception {
        // WHEN el administrador da de alta un profesional sin apellido
        postJson("/profesionales", """
                {"nombre":"Ana","matricula":"MP-0001"}""")
                // THEN el alta se rechaza con 422 y DATOS_INVALIDOS, AND indica el campo apellido
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("apellido")));
    }
}
