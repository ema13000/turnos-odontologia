package ar.turnos.odontologia.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MargenTest {

    @Test
    @DisplayName("Margen menor al mínimo")
    void margenMenorAlMinimo() {
        // GIVEN una configuración de margen de 8 minutos
        // WHEN se intenta usar ese margen para crear turnos
        // THEN la configuración se rechaza con MARGEN_INVALIDO
        assertThatThrownBy(() -> Margen.deMinutos(8))
                .isInstanceOf(TurnoRechazadoException.class)
                .extracting(e -> ((TurnoRechazadoException) e).codigo())
                .isEqualTo(CodigoRechazo.MARGEN_INVALIDO);
    }

    @Test
    @DisplayName("El margen predeterminado es de 10 minutos")
    void margenPredeterminado() {
        assertThat(Margen.PREDETERMINADO.valor()).isEqualTo(Duration.ofMinutes(10));
    }
}
