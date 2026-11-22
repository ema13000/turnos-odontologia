package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.NumeroReserva;

/** Doble de prueba de {@link GeneradorNumeroReserva}: números distintos y deterministas. */
class GeneradorNumeroReservaSecuencial implements GeneradorNumeroReserva {

    private long siguiente = 0;

    @Override
    public NumeroReserva generar() {
        long n = siguiente++;
        char[] simbolos = new char[NumeroReserva.LARGO];
        int base = NumeroReserva.ALFABETO.length();
        for (int i = simbolos.length - 1; i >= 0; i--) {
            simbolos[i] = NumeroReserva.ALFABETO.charAt((int) (n % base));
            n /= base;
        }
        return NumeroReserva.deSimbolos(new String(simbolos));
    }
}
