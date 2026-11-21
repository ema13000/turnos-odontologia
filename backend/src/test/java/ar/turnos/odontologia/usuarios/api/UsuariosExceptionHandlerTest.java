package ar.turnos.odontologia.usuarios.api;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Traducción de las violaciones de unicidad por carrera (design D7): el resultado no depende de quién llega primero. */
class UsuariosExceptionHandlerTest {

    private final UsuariosExceptionHandler manejador = new UsuariosExceptionHandler();

    private static DataIntegrityViolationException violacion(String restriccion) {
        return new DataIntegrityViolationException("no se pudo guardar",
                new ConstraintViolationException("duplicado", new SQLException("duplicate key", "23505"),
                        "insert", restriccion));
    }

    @Test
    @DisplayName("La restricción paciente_dni_key se traduce a 409 DNI_YA_REGISTRADO")
    void dniPorCarrera() {
        ProblemDetail problema = manejador.violacionDeIntegridad(violacion("paciente_dni_key"));

        assertThat(problema.getStatus()).isEqualTo(409);
        assertThat(problema.getProperties()).containsEntry("codigo", "DNI_YA_REGISTRADO");
    }

    @Test
    @DisplayName("La restricción usuario_username_unico se traduce a 409 EMAIL_YA_REGISTRADO")
    void emailPorCarrera() {
        ProblemDetail problema = manejador.violacionDeIntegridad(violacion("usuario_username_unico"));

        assertThat(problema.getStatus()).isEqualTo(409);
        assertThat(problema.getProperties()).containsEntry("codigo", "EMAIL_YA_REGISTRADO");
    }

    @Test
    @DisplayName("Una restricción desconocida no se traduce: se vuelve a lanzar")
    void restriccionDesconocida() {
        DataIntegrityViolationException desconocida = violacion("otra_restriccion");

        assertThatThrownBy(() -> manejador.violacionDeIntegridad(desconocida)).isSameAs(desconocida);
    }
}
