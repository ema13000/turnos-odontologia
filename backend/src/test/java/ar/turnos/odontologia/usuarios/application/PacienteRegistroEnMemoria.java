package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Doble de prueba de {@link PacienteRegistro}. */
public class PacienteRegistroEnMemoria implements PacienteRegistro {

    private final List<PerfilPaciente> pacientes = new ArrayList<>();

    /** Paciente cargado por el consultorio (sin cuenta). */
    public long cargar(PerfilPaciente perfil) {
        pacientes.add(perfil);
        return pacientes.size();
    }

    @Override
    public Optional<PerfilPaciente> buscarPerfil(long pacienteId) {
        return pacienteId >= 1 && pacienteId <= pacientes.size()
                ? Optional.of(pacientes.get((int) pacienteId - 1)) : Optional.empty();
    }

    @Override
    public boolean existePorDni(String dni) {
        return pacientes.stream().anyMatch(p -> p.dni().equals(dni));
    }

    @Override
    public long crear(PerfilPaciente perfil) {
        return cargar(perfil);
    }

    public List<PerfilPaciente> todos() {
        return List.copyOf(pacientes);
    }
}
