package ar.turnos.odontologia.usuarios.domain;

/** Datos del paciente que él mismo puede ver. */
public record PerfilPaciente(String nombre, String apellido, String dni, String email, String telefono) {
}
