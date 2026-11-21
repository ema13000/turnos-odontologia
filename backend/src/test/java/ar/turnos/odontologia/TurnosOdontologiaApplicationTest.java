package ar.turnos.odontologia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Desde C-03 el contexto necesita PostgreSQL (JPA y Flyway), así que es un test de integración:
 * corre con {@code ./gradlew integrationTest} contra la base de tests, no con {@code ./gradlew test}.
 */
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class TurnosOdontologiaApplicationTest {

    @Test
    @DisplayName("El contexto de la aplicación arranca")
    void contextoArranca() {
        // Falla si el contexto de Spring no puede iniciarse.
    }
}
