package ar.turnos.odontologia.usuarios.domain;

/**
 * Cuenta de acceso. {@code id} es nulo hasta que se guarda; {@code passwordHash} es siempre un hash BCrypt.
 * Un usuario PACIENTE tiene {@code pacienteId}; un ODONTOLOGO tiene {@code profesionalId}.
 */
public record Usuario(
        Long id,
        NombreUsuario username,
        String passwordHash,
        Rol rol,
        Long profesionalId,
        Long pacienteId,
        boolean activo) {
}
