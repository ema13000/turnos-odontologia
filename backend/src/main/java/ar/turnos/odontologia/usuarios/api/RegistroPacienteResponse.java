package ar.turnos.odontologia.usuarios.api;

import ar.turnos.odontologia.usuarios.domain.Rol;

public record RegistroPacienteResponse(String username, Rol rol) {
}
