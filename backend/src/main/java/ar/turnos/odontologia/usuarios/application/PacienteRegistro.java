package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;

import java.util.Optional;

/** Puerto hacia los datos de paciente que necesitan las cuentas de usuario. */
public interface PacienteRegistro {

    Optional<PerfilPaciente> buscarPerfil(long pacienteId);

    boolean existePorDni(String dni);

    /** Crea un paciente nuevo y devuelve su id. */
    long crear(PerfilPaciente paciente);
}
