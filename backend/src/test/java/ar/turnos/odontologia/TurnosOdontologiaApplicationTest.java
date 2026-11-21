package ar.turnos.odontologia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Arranca la aplicación completa. Desde C-03 eso incluye la conexión a PostgreSQL (DB_URL), así que
 * es un test de integración: corre con {@code ./gradlew integrationTest}, no con {@code ./gradlew test}.
 */
@Tag("integration")
@SpringBootTest
class TurnosOdontologiaApplicationTest {

    @Test
    @DisplayName("El contexto de la aplicación arranca")
    void contextoArranca() {
        // Falla si el contexto de Spring no puede iniciarse.
    }
}
