package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.NombreUsuario;
import ar.turnos.odontologia.usuarios.domain.Rol;
import ar.turnos.odontologia.usuarios.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrearAdminInicialTest {

    private static final String CLAVE = "clave-ficticia-admin";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioRepositoryEnMemoria usuarios;
    private CrearAdminInicial crearAdmin;

    @BeforeEach
    void preparar() {
        usuarios = new UsuarioRepositoryEnMemoria();
        crearAdmin = new CrearAdminInicial(usuarios, encoder);
    }

    @Test
    @DisplayName("Primer arranque con las variables definidas")
    void primerArranqueConLasVariablesDefinidas() {
        crearAdmin.ejecutar("admin", CLAVE);

        Usuario admin = usuarios.buscarPorUsername(NombreUsuario.de("admin")).orElseThrow();
        assertThat(admin.rol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.activo()).isTrue();
        assertThat(admin.passwordHash()).isNotEqualTo(CLAVE);
        assertThat(encoder.matches(CLAVE, admin.passwordHash())).isTrue();
        assertThat(usuarios.todos()).hasSize(1);
    }

    @Test
    @DisplayName("Ya existe un administrador")
    void yaExisteUnAdministrador() {
        usuarios.guardar(new Usuario(null, NombreUsuario.de("admin"), "hash-existente", Rol.ADMIN, null, null, true));

        crearAdmin.ejecutar("otro.admin", CLAVE);

        assertThat(usuarios.todos()).hasSize(1);
        assertThat(usuarios.buscarPorUsername(NombreUsuario.de("admin")).orElseThrow().passwordHash())
                .isEqualTo("hash-existente");
    }

    @Test
    @DisplayName("Variables sin definir")
    void variablesSinDefinir() {
        crearAdmin.ejecutar("admin", "");
        crearAdmin.ejecutar("admin", null);
        crearAdmin.ejecutar("", CLAVE);
        crearAdmin.ejecutar(null, null);

        assertThat(usuarios.todos()).isEmpty();
    }

    @Test
    @DisplayName("Un recepcionista existente no impide crear el primer administrador")
    void otroRolNoImpide() {
        usuarios.guardar(new Usuario(null, NombreUsuario.de("recepcion"), "hash", Rol.RECEPCIONISTA, null, null, true));

        crearAdmin.ejecutar("admin", CLAVE);

        assertThat(usuarios.existeConRol(Rol.ADMIN)).isTrue();
    }

    @Test
    @DisplayName("Una contraseña fuera de la política frena el arranque sin mostrarla")
    void contrasenaFueraDePolitica() {
        assertThatThrownBy(() -> crearAdmin.ejecutar("admin", "corta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("corta");
        assertThat(usuarios.todos()).isEmpty();
    }
}
