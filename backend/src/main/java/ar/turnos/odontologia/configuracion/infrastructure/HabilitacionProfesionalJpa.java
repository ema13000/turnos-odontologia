package ar.turnos.odontologia.configuracion.infrastructure;

import ar.turnos.odontologia.configuracion.application.HabilitacionProfesional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consulta la tabla {@code profesional_prestacion} (RN-AG-15). */
@Component
public class HabilitacionProfesionalJpa implements HabilitacionProfesional {

    private final ProfesionalRepository profesionales;

    public HabilitacionProfesionalJpa(ProfesionalRepository profesionales) {
        this.profesionales = profesionales;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaHabilitado(Long profesionalId, Long prestacionId) {
        return profesionales.estaHabilitado(profesionalId, prestacionId);
    }
}
