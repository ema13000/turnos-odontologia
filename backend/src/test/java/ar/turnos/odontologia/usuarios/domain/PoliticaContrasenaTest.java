package ar.turnos.odontologia.usuarios.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaContrasenaTest {

    @Test
    @DisplayName("7 caracteres no cumplen")
    void sieteNoCumple() {
        assertThat(PoliticaContrasena.cumple("a".repeat(7))).isFalse();
    }

    @Test
    @DisplayName("8 caracteres cumplen")
    void ochoCumple() {
        assertThat(PoliticaContrasena.cumple("a".repeat(8))).isTrue();
    }

    @Test
    @DisplayName("72 caracteres cumplen")
    void setentaYDosCumple() {
        assertThat(PoliticaContrasena.cumple("a".repeat(72))).isTrue();
    }

    @Test
    @DisplayName("73 caracteres no cumplen")
    void setentaYTresNoCumple() {
        assertThat(PoliticaContrasena.cumple("a".repeat(73))).isFalse();
    }

    @Test
    @DisplayName("72 caracteres que superan los 72 bytes de BCrypt no cumplen")
    void masDeSetentaYDosBytesNoCumple() {
        assertThat(PoliticaContrasena.cumple("ñ".repeat(72))).isFalse();
        assertThat(PoliticaContrasena.cumple("ñ".repeat(36))).isTrue();
    }

    @Test
    @DisplayName("Una contraseña nula no cumple")
    void nulaNoCumple() {
        assertThat(PoliticaContrasena.cumple(null)).isFalse();
    }
}
