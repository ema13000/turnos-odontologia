package ar.turnos.odontologia.agenda.domain.reglas;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Conjunto de franjas semanales de un profesional, sin superposiciones (design D1, D7). Un turno está dentro de horario
 * si, en hora local, empieza y termina el mismo día dentro de una misma franja (RN-AG-13, design D4).
 */
public final class HorarioSemanal {

    private static final HorarioSemanal SIN_LIMITES = new HorarioSemanal(List.of(), true);

    private final List<FranjaHoraria> franjas;
    private final boolean sinLimites;

    private HorarioSemanal(List<FranjaHoraria> franjas, boolean sinLimites) {
        this.franjas = List.copyOf(franjas);
        this.sinLimites = sinLimites;
    }

    /** @throws DisponibilidadException con {@code FRANJAS_SUPERPUESTAS} si dos franjas del mismo día se pisan */
    public static HorarioSemanal de(List<FranjaHoraria> franjas) {
        Objects.requireNonNull(franjas, "franjas");
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                if (franjas.get(i).seSuperponeCon(franjas.get(j))) {
                    throw new DisponibilidadException(CodigoDisponibilidad.FRANJAS_SUPERPUESTAS,
                            "Dos franjas del mismo día se superponen");
                }
            }
        }
        return new HorarioSemanal(franjas, false);
    }

    /**
     * Horario que acepta cualquier turno. Lo usan los constructores anteriores a C-07 de los casos de uso, para que
     * los tests de C-01, C-06 y C-08 no cambien de significado (design D2).
     */
    public static HorarioSemanal sinLimites() {
        return SIN_LIMITES;
    }

    public List<FranjaHoraria> franjas() {
        return franjas;
    }

    public boolean contiene(Intervalo intervalo, ZoneId zona) {
        if (sinLimites) {
            return true;
        }
        ZonedDateTime inicio = intervalo.inicio().atZone(zona);
        ZonedDateTime fin = intervalo.fin().atZone(zona);
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            return false;
        }
        return franjas.stream().anyMatch(franja ->
                franja.contiene(inicio.getDayOfWeek(), inicio.toLocalTime(), fin.toLocalTime()));
    }
}
