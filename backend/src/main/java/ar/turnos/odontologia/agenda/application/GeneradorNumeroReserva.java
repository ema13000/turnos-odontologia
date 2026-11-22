package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.NumeroReserva;

/** Puerto de salida: genera un número de reserva que ningún otro turno tiene. */
public interface GeneradorNumeroReserva {

    NumeroReserva generar();
}
