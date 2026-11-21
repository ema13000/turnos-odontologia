package ar.turnos.odontologia.usuarios.api;

import ar.turnos.odontologia.shared.security.IntegracionSeguridadBase;
import ar.turnos.odontologia.shared.security.Navegador;
import ar.turnos.odontologia.usuarios.application.PacienteRegistro;
import ar.turnos.odontologia.usuarios.application.UsuarioRepository;
import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;
import ar.turnos.odontologia.usuarios.domain.Rol;
import ar.turnos.odontologia.usuarios.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Escenarios del spec cuentas-pacientes. */
@Tag("integration")
class RegistroPacienteIntegrationTest extends IntegracionSeguridadBase {

    private static final String REGISTRO = "/api/v1/public/pacientes/registro";

    @Autowired
    PacienteRegistro pacientes;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    UsuariosExceptionHandler manejador;

    private String registro(String dni, String email, String clave) throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", "Paciente");
        datos.put("apellido", "Uno");
        datos.put("dni", dni);
        datos.put("email", email);
        datos.put("telefono", "1155550001");
        datos.put("password", clave);
        return json.writeValueAsString(datos);
    }

    private Navegador conTokenCsrf() throws Exception {
        Navegador navegador = navegador();
        navegador.get(SESION);
        return navegador;
    }

    private int contarUsuarios(String username) {
        return jdbc.queryForObject("SELECT count(*) FROM usuario WHERE username = ?", Integer.class, username);
    }

    private int contarPacientes(String dni) {
        return jdbc.queryForObject("SELECT count(*) FROM paciente WHERE dni = ?", Integer.class, dni);
    }

    // --- Registro del paciente con su cuenta

    @Test
    @DisplayName("Registro correcto")
    void registroCorrecto() throws Exception {
        String clave = contrasenaFicticia();
        Navegador navegador = conTokenCsrf();

        String cuerpo = navegador.post(REGISTRO, registro("99000001", "Paciente.Uno@Example.com", clave))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("paciente.uno@example.com"))
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(clave).doesNotContainIgnoringCase("password").doesNotContain("$2");
        Navegador paciente = iniciarSesion("paciente.uno@example.com", clave);
        paciente.get("/api/v1/paciente/perfil")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("99000001"));
    }

    @Test
    @DisplayName("El registro no inicia la sesión")
    void elRegistroNoIniciaLaSesion() throws Exception {
        Navegador navegador = conTokenCsrf();

        navegador.post(REGISTRO, registro("99000001", "paciente.uno@example.com", contrasenaFicticia()))
                .andExpect(status().isCreated());

        navegador.get(SESION).andExpect(jsonPath("$.autenticado").value(false));
    }

    @Test
    @DisplayName("Hash guardado al crear un usuario")
    void hashGuardadoAlCrearUnUsuario() throws Exception {
        String clave = contrasenaFicticia();
        Navegador navegador = conTokenCsrf();

        navegador.post(REGISTRO, registro("99000001", "paciente.uno@example.com", clave))
                .andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE username = ?", String.class,
                "paciente.uno@example.com");
        assertThat(hash).startsWith("$2").isNotEqualTo(clave);
        assertThat(passwordEncoder.matches(clave, hash)).isTrue();
    }

    // --- Registro único por DNI

    @Test
    @DisplayName("DNI de un paciente con cuenta")
    void dniDeUnPacienteConCuenta() throws Exception {
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", contrasenaFicticia());
        Navegador navegador = conTokenCsrf();

        navegador.post(REGISTRO, registro("99000001", "otro.email@example.com", contrasenaFicticia()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_YA_REGISTRADO"));

        assertThat(contarUsuarios("otro.email@example.com")).isZero();
    }

    @Test
    @DisplayName("DNI de un paciente cargado por el consultorio")
    void dniDeUnPacienteCargadoPorElConsultorio() throws Exception {
        long id = crearPacienteSinCuenta("Paciente", "Cargado", "99000001", null);
        Navegador navegador = conTokenCsrf();

        navegador.post(REGISTRO, registro("99000001", "paciente.uno@example.com", contrasenaFicticia()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_YA_REGISTRADO"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE paciente_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT apellido FROM paciente WHERE id = ?", String.class, id))
                .isEqualTo("Cargado");
        assertThat(contarPacientes("99000001")).isEqualTo(1);
    }

    @Test
    @DisplayName("Una carrera por el mismo DNI o email se traduce según la restricción real de la base")
    void carreraTraducidaConLasRestriccionesReales() {
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", contrasenaFicticia());
        // se saltea la verificación previa, como lo haría una carrera entre dos registros
        var paciente = new PerfilPaciente("Otro", "Dos", "99000001", null, null);
        var usuario = new Usuario(null,
                NombreUsuario.de("paciente.uno@example.com"), "$2a$10$hash",
                Rol.ADMIN, null, null, true);

        var porDni = assertThrows(
                DataIntegrityViolationException.class, () -> pacientes.crear(paciente));
        var porEmail = assertThrows(
                DataIntegrityViolationException.class, () -> usuarios.guardar(usuario));

        assertThat(manejador.violacionDeIntegridad(porDni).getProperties()).containsEntry("codigo", "DNI_YA_REGISTRADO");
        assertThat(manejador.violacionDeIntegridad(porEmail).getProperties()).containsEntry("codigo", "EMAIL_YA_REGISTRADO");
    }

    // --- Email único como usuario

    @Test
    @DisplayName("Email ya usado por otra cuenta")
    void emailYaUsadoPorOtraCuenta() throws Exception {
        crearPacienteConCuenta("Paciente", "Uno", "99000001", "paciente.uno@example.com", contrasenaFicticia());
        Navegador navegador = conTokenCsrf();

        navegador.post(REGISTRO, registro("99000002", "PACIENTE.UNO@example.com", contrasenaFicticia()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_YA_REGISTRADO"));

        assertThat(contarPacientes("99000002")).isZero();
    }

    // --- Validación de los datos del registro

    @Test
    @DisplayName("Contraseña demasiado corta")
    void contrasenaDemasiadoCorta() throws Exception {
        Navegador navegador = conTokenCsrf();

        navegador.post(REGISTRO, registro("99000001", "paciente.uno@example.com", "a".repeat(7)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'password')]").exists());

        assertThat(contarPacientes("99000001")).isZero();
    }

    @Test
    @DisplayName("Contraseña en el límite mínimo")
    void contrasenaEnElLimiteMinimo() throws Exception {
        conTokenCsrf().post(REGISTRO, registro("99000001", "paciente.uno@example.com", "a".repeat(8)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Contraseña demasiado larga")
    void contrasenaDemasiadoLarga() throws Exception {
        conTokenCsrf().post(REGISTRO, registro("99000001", "paciente.uno@example.com", "a".repeat(73)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'password')]").exists());
    }

    @Test
    @DisplayName("DNI y email con formato inválido")
    void dniYEmailConFormatoInvalido() throws Exception {
        conTokenCsrf().post(REGISTRO, registro("99-000", "no-es-un-email", contrasenaFicticia()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'dni')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'email')]").exists());
    }

    @Test
    @DisplayName("Faltan datos obligatorios")
    void faltanDatosObligatorios() throws Exception {
        String sinNombreNiApellido = json.writeValueAsString(Map.of(
                "dni", "99000001", "email", "paciente.uno@example.com", "password", contrasenaFicticia()));

        conTokenCsrf().post(REGISTRO, sinNombreNiApellido)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'apellido')]").exists());
    }
}
