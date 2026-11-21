package ar.turnos.odontologia.usuarios.application;

import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil del paciente identificado por su sesión: nunca recibe un id desde el pedido. */
@Service
public class ConsultarPerfilPaciente {

    private final PacienteRegistro pacientes;

    public ConsultarPerfilPaciente(PacienteRegistro pacientes) {
        this.pacientes = pacientes;
    }

    @Transactional(readOnly = true)
    public PerfilPaciente de(long pacienteIdDeLaSesion) {
        return pacientes.buscarPerfil(pacienteIdDeLaSesion)
                .orElseThrow(() -> new IllegalStateException("La sesión apunta a un paciente inexistente"));
    }
}
