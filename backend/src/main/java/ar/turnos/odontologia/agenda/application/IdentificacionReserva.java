package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.CodigoRechazo;
import ar.turnos.odontologia.agenda.domain.Turno;
import ar.turnos.odontologia.agenda.domain.TurnoInexistenteException;
import ar.turnos.odontologia.agenda.domain.TurnoRechazadoException;

/**
 * Identifica el turno que el personal quiere cancelar o reprogramar con el DNI del paciente y el número
 * de reserva (RN-AG-07). El error es el mismo para los dos datos, así no se puede probar uno por vez.
 */
final class IdentificacionReserva {

    private IdentificacionReserva() {}

    static Turno turnoActivo(TurnoRepository turnos, Long turnoId, String dni, String numeroReserva) {
        TurnoRepository.TurnoModificable encontrado = turnos.buscarParaModificar(turnoId)
                .orElseThrow(() -> new TurnoInexistenteException(turnoId));
        Turno turno = encontrado.turno();

        boolean dniCoincide = dni != null && dni.trim().equals(encontrado.dniPaciente());
        boolean numeroCoincide = turno.numeroReserva().coincideCon(numeroReserva);
        if (!(dniCoincide & numeroCoincide)) {
            throw new TurnoRechazadoException(CodigoRechazo.DATOS_DE_RESERVA_INVALIDOS,
                    "El DNI o el número de reserva no corresponden al turno");
        }
        if (!turno.esActivo()) {
            throw new TurnoRechazadoException(CodigoRechazo.TURNO_NO_ACTIVO,
                    "El turno no está activo: está " + turno.estado());
        }
        return turno;
    }
}
