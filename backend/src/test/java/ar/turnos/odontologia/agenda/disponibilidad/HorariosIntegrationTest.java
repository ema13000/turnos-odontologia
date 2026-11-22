package ar.turnos.odontologia.agenda.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Un test por escenario de horarios de trabajo del spec {@code disponibilidad-profesional} (C-07). */
class HorariosIntegrationTest extends IntegracionDisponibilidadBase {

    private static final List<String> LUNES_A_VIERNES_8_A_13 =
            List.of("1 08:00-13:00", "2 08:00-13:00", "3 08:00-13:00", "4 08:00-13:00", "5 08:00-13:00");

    @Test
    @DisplayName("Dos franjas el mismo día")
    void dosFranjasElMismoDia() throws Exception {
        MvcResult resultado = definirHorarios(como("admin"), draA,
                List.of(franja(2, "08:00", "12:00"), franja(2, "16:00", "20:00")));

        assertThat(status(resultado)).isEqualTo(200);
        assertThat(horariosDe(draA)).containsExactly("2 08:00-12:00", "2 16:00-20:00");
    }

    @Test
    @DisplayName("Redefinir reemplaza las franjas anteriores")
    void redefinirReemplazaLasFranjasAnteriores() throws Exception {
        cargarDeLunesAViernes(draA, "08:00", "13:00");

        definirHorarios(como("admin"), draA, List.of(franja(3, "09:00", "12:00")));

        assertThat(horariosDe(draA)).containsExactly("3 09:00-12:00");
    }

    @Test
    @DisplayName("Quitar todos los horarios")
    void quitarTodosLosHorarios() throws Exception {
        cargarDeLunesAViernes(draA, "08:00", "13:00");

        MvcResult resultado = definirHorarios(como("admin"), draA, List.of());

        assertThat(status(resultado)).isEqualTo(200);
        assertThat(horariosDe(draA)).isEmpty();
    }

    @Test
    @DisplayName("Franja con el fin antes del inicio")
    void franjaConElFinAntesDelInicio() throws Exception {
        MvcResult resultado = definirHorarios(como("admin"), draA, List.of(franja(2, "12:00", "08:00")));

        assertThat(status(resultado)).isEqualTo(422);
        assertThat(codigo(resultado)).isEqualTo("HORARIO_INVALIDO");
    }

    @Test
    @DisplayName("Franjas superpuestas en el mismo día")
    void franjasSuperpuestasEnElMismoDia() throws Exception {
        cargarDeLunesAViernes(draA, "08:00", "13:00");

        MvcResult resultado = definirHorarios(como("admin"), draA,
                List.of(franja(2, "08:00", "12:00"), franja(2, "11:00", "14:00")));

        assertThat(status(resultado)).isEqualTo(422);
        assertThat(codigo(resultado)).isEqualTo("FRANJAS_SUPERPUESTAS");
        assertThat(horariosDe(draA)).containsExactlyElementsOf(LUNES_A_VIERNES_8_A_13);
    }

    @Test
    @DisplayName("Franjas contiguas")
    void franjasContiguas() throws Exception {
        MvcResult resultado = definirHorarios(como("admin"), draA,
                List.of(franja(2, "08:00", "12:00"), franja(2, "12:00", "14:00")));

        assertThat(status(resultado)).isEqualTo(200);
        assertThat(horariosDe(draA)).containsExactly("2 08:00-12:00", "2 12:00-14:00");
    }

    @Test
    @DisplayName("La recepción consulta los horarios")
    void laRecepcionConsultaLosHorarios() throws Exception {
        cargarDeLunesAViernes(draA, "08:00", "13:00");

        MvcResult resultado = como("recepcion").get(horarios(draA)).andReturn();

        assertThat(status(resultado)).isEqualTo(200);
        assertThat(franjasDe(cuerpo(resultado))).containsExactlyElementsOf(LUNES_A_VIERNES_8_A_13);
    }

    @Test
    @DisplayName("La recepción intenta modificar los horarios")
    void laRecepcionIntentaModificarLosHorarios() throws Exception {
        MvcResult resultado = definirHorarios(como("recepcion"), draA, List.of(franja(2, "08:00", "12:00")));

        assertThat(status(resultado)).isEqualTo(403);
        assertThat(codigo(resultado)).isEqualTo("ACCESO_DENEGADO");
        assertThat(horariosDe(draA)).isEmpty();
    }

    @Test
    @DisplayName("Un odontólogo consulta los horarios de otro")
    void unOdontologoConsultaLosHorariosDeOtro() throws Exception {
        MvcResult resultado = como("bruno.prueba").get(horarios(draA)).andReturn();

        assertThat(status(resultado)).isEqualTo(403);
        assertThat(codigo(resultado)).isEqualTo("ACCESO_DENEGADO");
        // y sí puede consultar los suyos
        assertThat(status(como("bruno.prueba").get(horarios(drB)).andReturn())).isEqualTo(200);
    }

    @Test
    @DisplayName("Horarios de un profesional inexistente")
    void horariosDeUnProfesionalInexistente() throws Exception {
        MvcResult resultado = como("admin").get(horarios(999999)).andReturn();

        assertThat(status(resultado)).isEqualTo(404);
        assertThat(codigo(resultado)).isEqualTo("RECURSO_NO_ENCONTRADO");
    }

    private List<String> horariosDe(long profesional) throws Exception {
        return franjasDe(cuerpo(como("admin").get(horarios(profesional)).andReturn()));
    }
}
