package ar.turnos.odontologia.usuarios.api;

import ar.turnos.odontologia.usuarios.domain.Rol;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Estado de la sesión. Sin sesión, {@code username} y {@code rol} no se incluyen. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SesionResponse(boolean autenticado, String username, Rol rol) {

    public static SesionResponse sinSesion() {
        return new SesionResponse(false, null, null);
    }

    public static SesionResponse de(String username, Rol rol) {
        return new SesionResponse(true, username, rol);
    }
}
