package ar.turnos.odontologia.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades {@code app.seguridad}: orígenes CORS y administrador inicial. La contraseña del administrador
 * inicial viene solo del entorno (APP_ADMIN_PASSWORD), nunca del repositorio.
 */
@ConfigurationProperties("app.seguridad")
public record SeguridadProperties(List<String> corsOrigenes, AdminInicial adminInicial) {

    public SeguridadProperties {
        corsOrigenes = corsOrigenes == null ? List.of() : List.copyOf(corsOrigenes);
        adminInicial = adminInicial == null ? new AdminInicial("", "") : adminInicial;
    }

    public record AdminInicial(String username, String password) {
        public AdminInicial {
            username = username == null ? "" : username;
            password = password == null ? "" : password;
        }

        @Override
        public String toString() {
            return "AdminInicial[username=" + username + ", password=***]";
        }
    }
}
