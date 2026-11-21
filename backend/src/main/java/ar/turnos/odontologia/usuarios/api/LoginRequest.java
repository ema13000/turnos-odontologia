package ar.turnos.odontologia.usuarios.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {

    /** Nunca incluye la contraseña, para que no salga en un log por accidente. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
