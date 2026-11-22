package ar.turnos.odontologia.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.turnos.odontologia.notificaciones.application.NotificacionPort;
import ar.turnos.odontologia.notificaciones.application.NotificacionPort.EnvioNotificacion;
import ar.turnos.odontologia.notificaciones.application.NotificacionPort.ResultadoEnvio;
import ar.turnos.odontologia.notificaciones.domain.Canal;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Escenario "Registro de un envío simulado" del spec {@code notificaciones-recordatorios} (C-11, DD-04). */
class AdaptadoresSimuladosTest {

    private static final String CUERPO = "Hola Paciente Uno, te recordamos tu turno. Confirmá acá: https://turnos.example.test/confirmar/TOKEN-SECRETO-FICTICIO";
    private static final String TELEFONO = "+54 11 5555-0001";
    private static final String EMAIL = "paciente.uno@example.com";

    private final Logger raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> registro = new ListAppender<>();
    private Level nivelAnterior;

    @BeforeEach
    void capturarElLog() {
        registro.start();
        nivelAnterior = raiz.getLevel();
        raiz.setLevel(Level.ALL);
        raiz.addAppender(registro);
    }

    @AfterEach
    void soltarElLog() {
        raiz.detachAppender(registro);
        raiz.setLevel(nivelAnterior);
    }

    private String todoLoRegistrado() {
        return registro.list.stream()
                .map(e -> e.getLevel() + " " + e.getFormattedMessage() + " " + e.getThrowableProxy())
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private void assertSinDatos(String log) {
        assertThat(log).doesNotContain(CUERPO, "TOKEN-SECRETO-FICTICIO", "confirmar/", "Paciente Uno", TELEFONO,
                "5555", EMAIL, "paciente.uno");
    }

    @Test
    @DisplayName("Registro de un envío simulado")
    void registroDeUnEnvioSimulado() {
        // WHEN un adaptador simulado envía un mensaje
        ResultadoEnvio porWhatsapp = new WhatsAppAdapter(false).enviar(new EnvioNotificacion(7L, TELEFONO, CUERPO));
        ResultadoEnvio porEmail = new EmailAdapter(false).enviar(new EnvioNotificacion(8L, EMAIL, CUERPO));

        assertThat(porWhatsapp).isEqualTo(ResultadoEnvio.OK);
        assertThat(porEmail).isEqualTo(ResultadoEnvio.OK);
        // THEN el log registra solo el canal, el id de la notificación y el resultado
        List<String> lineas = registro.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(lineas).anyMatch(l -> l.contains("WHATSAPP") && l.contains("7") && l.contains("OK"));
        assertThat(lineas).anyMatch(l -> l.contains("EMAIL") && l.contains("8") && l.contains("OK"));
        // AND no contiene el cuerpo del mensaje, el token, el teléfono ni el email
        assertSinDatos(todoLoRegistrado());
    }

    @Test
    void ningunDatoApareceAunqueElEnvioFalle() {
        ResultadoEnvio porWhatsapp = new WhatsAppAdapter(true).enviar(new EnvioNotificacion(7L, TELEFONO, CUERPO));
        ResultadoEnvio porEmail = new EmailAdapter(true).enviar(new EnvioNotificacion(8L, EMAIL, CUERPO));

        assertThat(porWhatsapp).isEqualTo(ResultadoEnvio.FALLO);
        assertThat(porEmail).isEqualTo(ResultadoEnvio.FALLO);
        assertThat(todoLoRegistrado()).contains("FALLO");
        assertSinDatos(todoLoRegistrado());
    }

    @Test
    void cadaAdaptadorAtiendeSuCanal() {
        NotificacionPort whatsapp = new WhatsAppAdapter(false);
        NotificacionPort email = new EmailAdapter(false);

        assertThat(whatsapp.canal()).isEqualTo(Canal.WHATSAPP);
        assertThat(email.canal()).isEqualTo(Canal.EMAIL);
    }

    @Test
    void elEnvioNoExponeCuerpoNiDestinoAlImprimirse() {
        assertSinDatos(new EnvioNotificacion(7L, TELEFONO, CUERPO).toString());
    }
}
