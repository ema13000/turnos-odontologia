package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.IntegracionNotificacionesBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.TestPropertySource;

/**
 * El complemento de "con la tarea apagada nada corre solo": con {@code app.notificaciones.activo=true} la tarea
 * existe y la programación está activada. El intervalo es de una hora, así que no corre durante el test.
 */
@Tag("integration")
@TestPropertySource(properties = {"app.notificaciones.activo=true", "app.notificaciones.intervalo-ms=3600000"})
class ProgramacionActivaIntegrationTest extends IntegracionNotificacionesBase {

    @Autowired ApplicationContext contexto;

    @Test
    void conLaTareaActivaSeProgramaElEnvioDeRecordatorios() {
        assertThat(contexto.getBeansOfType(TareaRecordatorios.class)).hasSize(1);
        assertThat(contexto.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).hasSize(1);
    }
}
