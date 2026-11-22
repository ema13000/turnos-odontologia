package ar.turnos.odontologia.agenda.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas de punta a punta (tarea 5.3): un período registrado por la API frena la creación de turnos por la API,
 * con las reglas conectadas en {@code CrearTurno} y el adaptador JPA.
 */
class DisponibilidadEnLaApiIntegrationTest extends IntegracionDisponibilidadBase {

    @Test
    @DisplayName("Crear un turno durante una licencia registrada por la API")
    void turnoDuranteUnaLicencia() throws Exception {
        cargarDeLunesAViernes(draA, "08:00", "13:00");
        idCreado(registrarAusencia(como("recepcion"), draA, "LICENCIA", diciembre(1, 0, 0), diciembre(2, 0, 0)));

        MvcResult resultado = crearTurno(como("recepcion"), pacienteUno, draA, sillon1, diciembre(1, 10, 0));

        assertThat(status(resultado)).isEqualTo(409);
        assertThat(codigo(resultado)).isEqualTo("PROFESIONAL_AUSENTE");
    }

    @Test
    @DisplayName("Crear un turno durante un bloqueo registrado por la API")
    void turnoDuranteUnBloqueo() throws Exception {
        cargarDeLunesAViernes(draA, "08:00", "13:00");
        idCreado(bloquear(como("ana.ejemplo"), draA, diciembre(1, 11, 0), diciembre(1, 12, 0)));

        MvcResult resultado = crearTurno(como("recepcion"), pacienteUno, draA, sillon1, diciembre(1, 10, 45));

        assertThat(status(resultado)).isEqualTo(409);
        assertThat(codigo(resultado)).isEqualTo("AGENDA_BLOQUEADA");
    }

    @Test
    @DisplayName("Crear un turno con un profesional sin horarios por la API")
    void turnoSinHorarios() throws Exception {
        MvcResult resultado = crearTurno(como("recepcion"), pacienteUno, drB, sillon1, diciembre(1, 10, 0));

        assertThat(status(resultado)).isEqualTo(409);
        assertThat(codigo(resultado)).isEqualTo("FUERA_DE_HORARIO");
    }
}
