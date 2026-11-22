package ar.turnos.odontologia.configuracion.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionEntity, String> {
}
