package ar.turnos.odontologia.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.turnos.odontologia.agenda.domain.NumeroReserva;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GeneradorNumeroReservaAleatorioTest {

    @Test
    void generaNumerosConElFormatoYSinAmbiguos() {
        GeneradorNumeroReservaAleatorio generador = new GeneradorNumeroReservaAleatorio(new SecureRandom(), n -> false);
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            NumeroReserva numero = generador.generar();
            assertThat(numero.valor()).matches("[A-Z2-9]{4}-[A-Z2-9]{4}").doesNotContain("0", "O", "1", "I", "L");
            vistos.add(numero.valor());
        }
        assertThat(vistos).hasSize(1000);
    }

    @Test
    void descartaUnNumeroYaUsadoYPruebaOtro() {
        AtomicInteger consultas = new AtomicInteger();
        GeneradorNumeroReservaAleatorio generador = new GeneradorNumeroReservaAleatorio(new SecureRandom(),
                n -> consultas.incrementAndGet() == 1);

        assertThat(generador.generar()).isNotNull();
        assertThat(consultas).hasValue(2);
    }

    @Test
    void fallaSiNoEncuentraUnNumeroLibre() {
        GeneradorNumeroReservaAleatorio generador = new GeneradorNumeroReservaAleatorio(new SecureRandom(), n -> true);
        assertThatThrownBy(generador::generar).isInstanceOf(IllegalStateException.class);
    }
}
