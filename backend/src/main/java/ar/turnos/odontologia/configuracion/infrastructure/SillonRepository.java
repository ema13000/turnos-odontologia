package ar.turnos.odontologia.configuracion.infrastructure;

import ar.turnos.odontologia.shared.persistence.SillonEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SillonRepository extends JpaRepository<SillonEntity, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<SillonEntity> findAllByOrderByNombreAsc();
}
