package ar.turnos.odontologia.usuarios.infrastructure;

import ar.turnos.odontologia.shared.security.SeguridadProperties;
import ar.turnos.odontologia.usuarios.application.CrearAdminInicial;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar, crea el primer ADMIN con APP_ADMIN_USERNAME y APP_ADMIN_PASSWORD si hace falta. */
@Component
class CrearAdminInicialRunner implements ApplicationRunner {

    private final CrearAdminInicial crearAdminInicial;
    private final SeguridadProperties propiedades;

    CrearAdminInicialRunner(CrearAdminInicial crearAdminInicial, SeguridadProperties propiedades) {
        this.crearAdminInicial = crearAdminInicial;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        crearAdminInicial.ejecutar(propiedades.adminInicial().username(), propiedades.adminInicial().password());
    }
}
