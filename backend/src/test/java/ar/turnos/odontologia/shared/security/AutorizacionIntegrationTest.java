package ar.turnos.odontologia.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Escenarios del spec seguridad-autorizacion. */
@Tag("integration")
class AutorizacionIntegrationTest extends IntegracionSeguridadBase {

    @Test
    @DisplayName("Ruta pública sin sesión")
    void rutaPublicaSinSesion() throws Exception {
        navegador().get(SESION).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ruta del personal sin sesión")
    void rutaDelPersonalSinSesion() throws Exception {
        navegador().get("/api/v1/turnos")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Ruta del paciente sin sesión")
    void rutaDelPacienteSinSesion() throws Exception {
        navegador().get("/api/v1/paciente/perfil")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("El personal no usa las rutas del paciente")
    void elPersonalNoUsaLasRutasDelPaciente() throws Exception {
        String clave = contrasenaFicticia();
        crearPersonal("recepcion", "RECEPCIONISTA", clave);
        Navegador navegador = iniciarSesion("recepcion", clave);

        navegador.get("/api/v1/paciente/perfil")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Un paciente pide la agenda del personal")
    void unPacientePideLaAgendaDelPersonal() throws Exception {
        String clave = contrasenaFicticia();
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", clave);
        Navegador navegador = iniciarSesion("paciente.uno@example.com", clave);

        navegador.get("/api/v1/turnos")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Un paciente pide los datos de otro paciente por la ruta del personal")
    void unPacientePideLosDatosDeOtroPaciente() throws Exception {
        String clave = contrasenaFicticia();
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", clave);
        long idDos = crearPacienteConCuenta("Paciente", "Dos", "99000002", "paciente.dos@example.com", contrasenaFicticia());
        Navegador navegador = iniciarSesion("paciente.uno@example.com", clave);

        String cuerpo = navegador.get("/api/v1/pacientes/" + idDos)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("Dos").doesNotContain("99000002").doesNotContain("paciente.dos@example.com");
    }

    @Test
    @DisplayName("Ruta fuera de la API con sesión del administrador")
    void rutaFueraDeLaApiConSesionDelAdministrador() throws Exception {
        String clave = contrasenaFicticia();
        crearPersonal("admin", "ADMIN", clave);
        Navegador navegador = iniciarSesion("admin", clave);

        navegador.get("/actuator/env")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Operación de ADMIN pedida por recepción")
    void operacionDeAdminPedidaPorRecepcion() throws Exception {
        String clave = contrasenaFicticia();
        crearPersonal("recepcion", "RECEPCIONISTA", clave);
        Navegador navegador = iniciarSesion("recepcion", clave);

        navegador.get("/api/v1/prueba-seguridad/solo-admin")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Operación de ADMIN pedida por el administrador")
    void operacionDeAdminPedidaPorElAdministrador() throws Exception {
        String clave = contrasenaFicticia();
        crearPersonal("admin", "ADMIN", clave);
        Navegador navegador = iniciarSesion("admin", clave);

        navegador.get("/api/v1/prueba-seguridad/solo-admin").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Perfil propio")
    void perfilPropio() throws Exception {
        String clave = contrasenaFicticia();
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", clave);
        crearPacienteConCuenta("Paciente", "Dos", "99000002", "paciente.dos@example.com", contrasenaFicticia());
        Navegador navegador = iniciarSesion("paciente.uno@example.com", clave);

        String cuerpo = navegador.get("/api/v1/paciente/perfil")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Paciente"))
                .andExpect(jsonPath("$.apellido").value("Uno"))
                .andExpect(jsonPath("$.dni").value("99000001"))
                .andExpect(jsonPath("$.email").value("paciente.uno@example.com"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("Dos").doesNotContain("99000002");
    }

    @Test
    @DisplayName("Intento de pedir el perfil de otro paciente")
    void intentoDePedirElPerfilDeOtroPaciente() throws Exception {
        String clave = contrasenaFicticia();
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", clave);
        long idDos = crearPacienteConCuenta("Paciente", "Dos", "99000002", "paciente.dos@example.com", contrasenaFicticia());
        Navegador navegador = iniciarSesion("paciente.uno@example.com", clave);

        String cuerpo = navegador.get("/api/v1/paciente/perfil?pacienteId=" + idDos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("99000001"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("Dos").doesNotContain("99000002");
    }

    @Test
    @DisplayName("Origen configurado")
    void origenConfigurado() throws Exception {
        mvc.perform(options(LOGIN)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Origen no configurado")
    void origenNoConfigurado() throws Exception {
        mvc.perform(options(LOGIN)
                        .header("Origin", "http://otro-origen.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Ruta fuera de la API sin sesión")
    void rutaFueraDeLaApiSinSesion() throws Exception {
        navegador().get("/actuator/env")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
