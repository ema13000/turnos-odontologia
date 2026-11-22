package ar.turnos.odontologia.configuracion.application;

import ar.turnos.odontologia.shared.persistence.PrestacionEntity;
import ar.turnos.odontologia.shared.persistence.ProfesionalEntity;
import ar.turnos.odontologia.shared.persistence.SillonEntity;

/**
 * Datos que devuelven los casos de uso. Se arman dentro de la transacción, así la API nunca
 * recibe ni expone entidades JPA (regla dura 9).
 */
public final class Vistas {

    private Vistas() {
    }

    public record ProfesionalVista(Long id, String nombre, String apellido, String matricula,
                                   boolean activo, Long sillonHabitualId) {
        static ProfesionalVista de(ProfesionalEntity p) {
            Long sillon = p.getSillonHabitual() == null ? null : p.getSillonHabitual().getId();
            return new ProfesionalVista(p.getId(), p.getNombre(), p.getApellido(), p.getMatricula(),
                    p.isActivo(), sillon);
        }
    }

    public record SillonVista(Long id, String nombre, boolean activo) {
        static SillonVista de(SillonEntity s) {
            return new SillonVista(s.getId(), s.getNombre(), s.isActivo());
        }
    }

    public record PrestacionVista(Long id, String nombre, int duracionMinutos, boolean activo) {
        static PrestacionVista de(PrestacionEntity p) {
            return new PrestacionVista(p.getId(), p.getNombre(), p.getDuracionMinutos(), p.isActivo());
        }
    }
}
