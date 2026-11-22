package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.notificaciones.domain.Canal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Email simulado. {@code app.notificaciones.simulado.falla-email=true} lo hace fallar (solo para pruebas). */
@Component
class EmailAdapter extends CanalSimulado {

    EmailAdapter(@Value("${app.notificaciones.simulado.falla-email:false}") boolean falla) {
        super(falla);
    }

    @Override
    public Canal canal() {
        return Canal.EMAIL;
    }
}
