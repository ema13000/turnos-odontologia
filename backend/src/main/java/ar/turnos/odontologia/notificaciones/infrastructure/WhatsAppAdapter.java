package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.notificaciones.domain.Canal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** WhatsApp simulado. {@code app.notificaciones.simulado.falla-whatsapp=true} lo hace fallar (solo para pruebas). */
@Component
class WhatsAppAdapter extends CanalSimulado {

    WhatsAppAdapter(@Value("${app.notificaciones.simulado.falla-whatsapp:false}") boolean falla) {
        super(falla);
    }

    @Override
    public Canal canal() {
        return Canal.WHATSAPP;
    }
}
