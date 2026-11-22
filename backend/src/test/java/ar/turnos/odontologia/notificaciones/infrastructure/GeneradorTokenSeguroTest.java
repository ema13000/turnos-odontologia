package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.application.HuellaToken;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** El token es de 256 bits, seguro para un link, distinto cada vez, y su huella SHA-256 tiene 64 caracteres. */
class GeneradorTokenSeguroTest {

    private final GeneradorTokenSeguro generador = new GeneradorTokenSeguro(new SecureRandom());

    @Test
    void generaTokensDe256BitsEnBase64UrlSinRelleno() {
        String token = generador.generar();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void cadaTokenEsDistinto() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(generador.generar());
        }

        assertThat(tokens).hasSize(1_000);
    }

    @Test
    void laHuellaEsUnSha256HexadecimalYNoEsElToken() {
        String token = generador.generar();

        assertThat(HuellaToken.de(token)).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(token)
                .isEqualTo(HuellaToken.de(token));
        // valor conocido de SHA-256("abc")
        assertThat(HuellaToken.de("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
