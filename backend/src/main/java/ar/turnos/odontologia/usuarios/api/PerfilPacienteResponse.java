package ar.turnos.odontologia.usuarios.api;

import ar.turnos.odontologia.usuarios.domain.PerfilPaciente;

public record PerfilPacienteResponse(String nombre, String apellido, String dni, String email, String telefono) {

    static PerfilPacienteResponse de(PerfilPaciente perfil) {
        return new PerfilPacienteResponse(perfil.nombre(), perfil.apellido(), perfil.dni(), perfil.email(),
                perfil.telefono());
    }
}
