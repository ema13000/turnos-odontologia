package ar.turnos.odontologia.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class NumeroReservaTest {

    @Test
    void aceptaElFormatoConElAlfabetoSinAmbiguos() {
        assertThat(new NumeroReserva("K7M2-QX9P").valor()).isEqualTo("K7M2-QX9P");
    }

    @ParameterizedTest
    @ValueSource(strings = {"K7M0-QX9P", "K7MO-QX9P", "K7M1-QX9P", "K7MI-QX9P", "K7ML-QX9P",
            "K7M2QX9P", "K7M2-QX9", "k7m2-qx9p", "K7M2-QX9P-"})
    void rechazaCaracteresAmbiguosYOtrosFormatos(String valor) {
        assertThatThrownBy(() -> new NumeroReserva(valor)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coincideSinImportarMayusculasNiEspacios() {
        NumeroReserva numero = new NumeroReserva("K7M2-QX9P");
        assertThat(numero.coincideCon(" k7m2-qx9p ")).isTrue();
        assertThat(numero.coincideCon("K7M2-QX9Q")).isFalse();
        assertThat(numero.coincideCon(null)).isFalse();
    }
}
