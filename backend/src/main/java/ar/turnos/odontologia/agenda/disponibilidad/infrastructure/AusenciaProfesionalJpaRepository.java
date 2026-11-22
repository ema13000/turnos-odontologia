package ar.turnos.odontologia.agenda.disponibilidad.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AusenciaProfesionalJpaRepository extends JpaRepository<AusenciaProfesionalEntity, Long> {

    /** Las que se cruzan con {@code [desde, hasta)}: {@code inicio < hasta && desde < fin}. */
    @Query("select a from AusenciaProfesionalEntity a where a.profesionalId = :profesionalId"
            + " and a.inicio < :hasta and :desde < a.fin order by a.inicio asc")
    List<AusenciaProfesionalEntity> queSeCruzan(Long profesionalId, Instant desde, Instant hasta);

    List<AusenciaProfesionalEntity> findByProfesionalIdOrderByInicioAsc(Long profesionalId);
}
