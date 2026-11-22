package ar.turnos.odontologia.agenda.infrastructure;

import ar.turnos.odontologia.agenda.application.CancelarTurno;
import ar.turnos.odontologia.agenda.application.ConfirmarTurno;
import ar.turnos.odontologia.agenda.application.CrearTurno;
import ar.turnos.odontologia.agenda.application.GeneradorNumeroReserva;
import ar.turnos.odontologia.agenda.application.RecursosAgenda;
import ar.turnos.odontologia.agenda.application.ReprogramarTurno;
import ar.turnos.odontologia.agenda.application.TurnoRepository;
import ar.turnos.odontologia.agenda.domain.Margen;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma los casos de uso de la agenda, que no dependen de Spring (design D5, D8). El margen y la zona horaria
 * salen de propiedades hasta que C-04 agregue la tabla {@code configuracion}.
 */
@Configuration
class AgendaConfig {

    @Bean
    ZoneId zonaHoraria(@Value("${app.zona-horaria:America/Argentina/Buenos_Aires}") String zona) {
        return ZoneId.of(zona);
    }

    /** Los tests de integración lo reemplazan por un reloj fijo marcado como {@code @Primary}. */
    @Bean
    Clock reloj(ZoneId zonaHoraria) {
        return Clock.system(zonaHoraria);
    }

    /** Un margen menor a 10 minutos hace fallar el arranque con {@code MARGEN_INVALIDO} (RN-AG-04). */
    @Bean
    Margen margen(@Value("${app.agenda.margen-minutos:10}") int minutos) {
        return Margen.deMinutos(minutos);
    }

    @Bean
    GeneradorNumeroReserva generadorNumeroReserva(TurnoJpaRepository turnos) {
        return new GeneradorNumeroReservaAleatorio(new SecureRandom(), turnos::existsByCodigoReserva);
    }

    @Bean
    CrearTurno crearTurno(TurnoRepository turnos, RecursosAgenda recursos, GeneradorNumeroReserva numeros,
                          Clock reloj, Margen margen) {
        return new CrearTurno(turnos, recursos, numeros, reloj, margen);
    }

    @Bean
    CancelarTurno cancelarTurno(TurnoRepository turnos) {
        return new CancelarTurno(turnos);
    }

    @Bean
    ConfirmarTurno confirmarTurno(TurnoRepository turnos) {
        return new ConfirmarTurno(turnos);
    }

    @Bean
    ReprogramarTurno reprogramarTurno(TurnoRepository turnos, CrearTurno crearTurno) {
        return new ReprogramarTurno(turnos, crearTurno);
    }
}
