package ar.turnos.odontologia.agenda.infrastructure;

import ar.turnos.odontologia.agenda.application.SillonesDisponibles;
import ar.turnos.odontologia.shared.persistence.SillonEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Adaptador JPA de {@link SillonesDisponibles}: sillones activos en orden determinista por id (D1). */
@Repository
class SillonesDisponiblesJpa implements SillonesDisponibles {

    private final EntityManager em;

    SillonesDisponiblesJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Long> activos() {
        return em.createQuery("select s.id from SillonEntity s where s.activo = true order by s.id asc", Long.class)
                .getResultList();
    }
}
