package ar.turnos.odontologia.agenda.infrastructure;

import ar.turnos.odontologia.agenda.application.GeneradorNumeroReserva;
import ar.turnos.odontologia.agenda.domain.NumeroReserva;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Genera números de reserva con {@link SecureRandom} (design D3). Con 31⁸ combinaciones una repetición es
 * muy improbable; igual se descarta un número ya usado y se prueba otro. La restricción UNIQUE de la base es
 * la garantía final.
 */
class GeneradorNumeroReservaAleatorio implements GeneradorNumeroReserva {

    static final int INTENTOS = 3;

    private final SecureRandom azar;
    private final Predicate<String> yaExiste;

    GeneradorNumeroReservaAleatorio(SecureRandom azar, Predicate<String> yaExiste) {
        this.azar = Objects.requireNonNull(azar, "azar");
        this.yaExiste = Objects.requireNonNull(yaExiste, "yaExiste");
    }

    @Override
    public NumeroReserva generar() {
        for (int intento = 0; intento < INTENTOS; intento++) {
            NumeroReserva numero = sortear();
            if (!yaExiste.test(numero.valor())) {
                return numero;
            }
        }
        throw new IllegalStateException("No se pudo generar un número de reserva libre");
    }

    private NumeroReserva sortear() {
        char[] simbolos = new char[NumeroReserva.LARGO];
        for (int i = 0; i < simbolos.length; i++) {
            simbolos[i] = NumeroReserva.ALFABETO.charAt(azar.nextInt(NumeroReserva.ALFABETO.length()));
        }
        return NumeroReserva.deSimbolos(new String(simbolos));
    }
}
