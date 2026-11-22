package ar.turnos.odontologia.notificaciones.infrastructure;

import ar.turnos.odontologia.notificaciones.application.EnviarRecordatorios.Resumen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Capa fina de la tarea programada (design D7): cada 5 minutos (configurable) llama al caso de uso. La primera
 * corrida espera un intervalo completo, así el arranque no manda mensajes. Se apaga con
 * {@code app.notificaciones.activo=false}.
 */
@Component
@ConditionalOnProperty(name = "app.notificaciones.activo", havingValue = "true", matchIfMissing = true)
class TareaRecordatorios {

    private static final Logger LOG = LoggerFactory.getLogger(TareaRecordatorios.class);

    private final ServicioNotificaciones servicio;

    TareaRecordatorios(ServicioNotificaciones servicio) {
        this.servicio = servicio;
    }

    @Scheduled(fixedDelayString = "${app.notificaciones.intervalo-ms:300000}",
            initialDelayString = "${app.notificaciones.intervalo-ms:300000}")
    void enviarRecordatorios() {
        try {
            Resumen resumen = servicio.enviarRecordatorios();
            LOG.info("Recordatorios: nuevos={} reintentos={}", resumen.nuevas(), resumen.reintentadas());
        } catch (RuntimeException e) {
            // un error no corta la tarea: se reintenta en la próxima corrida. Solo el tipo, nunca datos del paciente.
            LOG.error("Falló la corrida de recordatorios: {}", e.getClass().getSimpleName());
        }
    }
}
