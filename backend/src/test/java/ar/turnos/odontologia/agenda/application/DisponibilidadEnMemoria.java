package ar.turnos.odontologia.agenda.application;

import ar.turnos.odontologia.agenda.domain.Intervalo;
import ar.turnos.odontologia.agenda.domain.reglas.FranjaHoraria;
import ar.turnos.odontologia.agenda.domain.reglas.HorarioSemanal;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodoSinAtencion;
import ar.turnos.odontologia.agenda.domain.reglas.PeriodosProfesional;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Doble en memoria del puerto {@link DisponibilidadProfesional} (C-07). Sin franjas cargadas, no hay horario. */
class DisponibilidadEnMemoria implements DisponibilidadProfesional {

    private record Periodo(Long profesionalId, PeriodoSinAtencion periodo) {}

    private final Map<Long, List<FranjaHoraria>> franjas = new HashMap<>();
    private final List<Periodo> periodos = new ArrayList<>();

    /** @param dia día ISO: 1 = lunes … 7 = domingo */
    DisponibilidadEnMemoria franja(Long profesionalId, int dia, LocalTime desde, LocalTime hasta) {
        franjas.computeIfAbsent(profesionalId, id -> new ArrayList<>()).add(FranjaHoraria.de(dia, desde, hasta));
        return this;
    }

    DisponibilidadEnMemoria deLunesAViernes(Long profesionalId, LocalTime desde, LocalTime hasta) {
        for (int dia = 1; dia <= 5; dia++) {
            franja(profesionalId, dia, desde, hasta);
        }
        return this;
    }

    DisponibilidadEnMemoria periodo(Long profesionalId, PeriodoSinAtencion.Tipo tipo, Instant inicio, Instant fin) {
        periodos.add(new Periodo(profesionalId, PeriodoSinAtencion.de(tipo, inicio, fin)));
        return this;
    }

    @Override
    public PeriodosProfesional periodos(Long profesionalId, Intervalo ventana) {
        return new PeriodosProfesional(
                HorarioSemanal.de(franjas.getOrDefault(profesionalId, List.of())),
                periodos.stream()
                        .filter(p -> p.profesionalId().equals(profesionalId) && p.periodo().seCruzaCon(ventana))
                        .map(Periodo::periodo)
                        .toList());
    }
}
