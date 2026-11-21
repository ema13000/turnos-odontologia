package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.EstadoTurno;
import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.Margen;
import ar.turnos.odontologia.agenda.domain.Turno;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Fixture de los tests de cancelar y reprogramar, con los mismos datos ficticios que {@code CrearTurnoTest}:
 * Dra. A, Dr. B, Sillón 1 y 2, "Consulta" de 30 minutos, margen de 10 minutos, 2026-12-01 en Buenos Aires.
 * "Paciente Uno" tiene el DNI {@code 99000001}.
 */
class AgendaEnMemoria {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    static final LocalDate DIA = LocalDate.of(2026, 12, 1);

    static final Long PACIENTE_UNO = 1L;
    static final String DNI_PACIENTE_UNO = "99000001";
    static final Long OTRO_PACIENTE = 2L;
    static final Long DRA_A = 1L;
    static final Long DR_B = 2L;
    static final Long SILLON_1 = 1L;
    static final Long SILLON_2 = 2L;
    static final Long CONSULTA = 1L;

    final TurnoRepositoryEnMemoria turnos = new TurnoRepositoryEnMemoria();
    final RecursosAgendaEnMemoria recursos = new RecursosAgendaEnMemoria();
    final GeneradorNumeroReservaSecuencial numeros = new GeneradorNumeroReservaSecuencial();

    AgendaEnMemoria() {
        recursos.agregarPrestacion(CONSULTA, Duration.ofMinutes(30));
        turnos.registrarDni(PACIENTE_UNO, DNI_PACIENTE_UNO);
        turnos.registrarDni(OTRO_PACIENTE, "99000002");
    }

    static Instant a(int hora, int minuto) {
        return DIA.atTime(LocalTime.of(hora, minuto)).atZone(BUENOS_AIRES).toInstant();
    }

    static Clock relojALas(int hora, int minuto) {
        return Clock.fixed(a(hora, minuto), BUENOS_AIRES);
    }

    CrearTurno crearTurno(Clock reloj) {
        return new CrearTurno(turnos, recursos, numeros, reloj, Margen.PREDETERMINADO);
    }

    CancelarTurno cancelarTurno() {
        return new CancelarTurno(turnos);
    }

    ReprogramarTurno reprogramarTurno(Clock reloj) {
        return new ReprogramarTurno(turnos, crearTurno(reloj));
    }

    /** Guarda un turno de "Consulta" de 30 minutos y lo devuelve con su id. */
    Turno existente(Long paciente, Long profesional, Long sillon, Instant inicio, EstadoTurno estado) {
        return turnos.guardar(new Turno(null, paciente, profesional, sillon, CONSULTA,
                new Intervalo(inicio, inicio.plus(Duration.ofMinutes(30))), estado, numeros.generar()));
    }

    Turno dePacienteUno(Instant inicio, EstadoTurno estado) {
        return existente(PACIENTE_UNO, DRA_A, SILLON_1, inicio, estado);
    }
}
