package ar.turnos.odontologia.notificaciones.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas, solo para esta tarea y solo si {@code app.notificaciones.activo} es verdadero
 * (por defecto lo es). Está en su propia clase y no en la de la aplicación, así los tests la apagan sin tocarla.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.notificaciones.activo", havingValue = "true", matchIfMissing = true)
class ProgramacionConfig {
}
