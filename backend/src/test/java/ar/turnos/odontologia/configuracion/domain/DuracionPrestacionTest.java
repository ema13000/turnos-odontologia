package ar.turnos.odontologia.configuracion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DuracionPrestacionTest {

    @ParameterizedTest(name = "{0} minutos es válido")
    @ValueSource(ints = {5, 30, 45, 60})
    @DisplayName("Acepta duraciones positivas y múltiplos de 5")
    void aceptaMultiplosDeCinco(int minutos) {
        assertThat(DuracionPrestacion.deMinutos(minutos).minutos()).isEqualTo(minutos);
    }

    @ParameterizedTest(name = "{0} minutos se rechaza")
    @ValueSource(ints = {0, -5, 32, 1})
    @DisplayName("Rechaza duraciones no positivas o que no son múltiplo de 5")
    void rechazaDuracionesInvalidas(int minutos) {
        assertThatThrownBy(() -> DuracionPrestacion.deMinutos(minutos))
                .isInstanceOf(ConfiguracionException.class)
                .extracting(e -> ((ConfiguracionException) e).codigo())
                .isEqualTo(CodigoError.DURACION_INVALIDA);
    }

    @Test
    @DisplayName("Una duración nula es un dato inválido")
    void duracionNula() {
        assertThatThrownBy(() -> DuracionPrestacion.deMinutos(null))
                .isInstanceOf(ConfiguracionException.class)
                .extracting(e -> ((ConfiguracionException) e).codigo())
                .isEqualTo(CodigoError.DURACION_INVALIDA);
    }
}
