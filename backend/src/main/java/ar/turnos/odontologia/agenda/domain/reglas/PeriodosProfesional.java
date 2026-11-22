package ar.turnos.odontologia.agenda.domain.reglas;

import java.util.List;
import java.util.Objects;

/**
 * Lo que la agenda necesita saber de un profesional para un turno: sus franjas semanales y los períodos sin atención
 * que se cruzan con la ventana consultada (design D1).
 */
public record PeriodosProfesional(HorarioSemanal horario, List<PeriodoSinAtencion> periodos) {

    private static final PeriodosProfesional SIN_RESTRICCIONES =
            new PeriodosProfesional(HorarioSemanal.sinLimites(), List.of());

    public PeriodosProfesional {
        Objects.requireNonNull(horario, "horario");
        periodos = List.copyOf(periodos);
    }

    /** Sin franjas ni períodos que limiten: es lo que usan los constructores anteriores a C-07 (design D2). */
    public static PeriodosProfesional sinRestricciones() {
        return SIN_RESTRICCIONES;
    }
}
