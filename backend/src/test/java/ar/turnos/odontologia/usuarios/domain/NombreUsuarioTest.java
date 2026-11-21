package ar.turnos.odontologia.usuarios.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NombreUsuarioTest {

    @Test
    @DisplayName("Se quitan los espacios y se pasa a minúsculas")
    void normaliza() {
        assertThat(NombreUsuario.de(" Recepcion ").valor()).isEqualTo("recepcion");
    }

    @Test
    @DisplayName("Un email se normaliza a minúsculas")
    void normalizaEmail() {
        assertThat(NombreUsuario.de("Paciente.Uno@Example.com").valor()).isEqualTo("paciente.uno@example.com");
    }

    @Test
    @DisplayName("Un nombre ya normalizado no cambia")
    void yaNormalizado() {
        assertThat(NombreUsuario.de("admin").valor()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Dos nombres que difieren solo en mayúsculas y espacios son iguales")
    void igualdad() {
        assertThat(NombreUsuario.de(" ADMIN")).isEqualTo(NombreUsuario.de("admin "));
    }

    @Test
    @DisplayName("Un nombre vacío o en blanco es rechazado")
    void vacioRechazado() {
        assertThatThrownBy(() -> NombreUsuario.de("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NombreUsuario.de("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NombreUsuario.de(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
