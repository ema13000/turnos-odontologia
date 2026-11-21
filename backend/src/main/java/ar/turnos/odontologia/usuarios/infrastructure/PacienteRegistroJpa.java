package ar.turnos.odontologia.usuarios.infrastructure;

import ar.turnos.odontologia.shared.persistence.PacienteEntity;
import ar.turnos.odontologia.usuarios.application.PacienteRegistro;
import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class PacienteRegistroJpa implements PacienteRegistro {

    private final PacienteCuentaJpaRepository pacientes;

    PacienteRegistroJpa(PacienteCuentaJpaRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Override
    public Optional<PerfilPaciente> buscarPerfil(long pacienteId) {
        return pacientes.findById(pacienteId)
                .map(p -> new PerfilPaciente(p.getNombre(), p.getApellido(), p.getDni(), p.getEmail(), p.getTelefono()));
    }

    @Override
    public boolean existePorDni(String dni) {
        return pacientes.existsByDni(dni);
    }

    @Override
    public long crear(PerfilPaciente p) {
        // saveAndFlush: una violación de unicidad por carrera se detecta acá, dentro de la transacción
        return pacientes.saveAndFlush(new PacienteEntity(p.nombre(), p.apellido(), p.dni(), p.email(), p.telefono()))
                .getId();
    }
}
