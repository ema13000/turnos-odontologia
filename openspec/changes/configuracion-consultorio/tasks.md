# Tasks

## 1. Base del módulo y migraciones

- [x] 1.1 Agregar `spring-boot-starter-web` y `spring-boot-starter-validation` a `backend/build.gradle`, y crear el perfil `test` en `application.yml` (con un `DB_URL` propio, recomendado `turnos_test`, y sin la carpeta `db/seed`); verificar que `./gradlew test` sigue en verde
- [x] 1.2 Crear `db/migration/V2__configuracion_consultorio.sql` (`profesional_prestacion`, `configuracion` con `margen_minutos = 10`, `profesional.sillon_habitual_id` y CHECK de duración múltiplo de 5, design D10); verificar con `./gradlew integrationTest` que Flyway migra y que los tests de C-03 siguen en verde
- [x] 1.3 Crear `db/seed/V2_1__seed_dev.sql` con los datos ficticios de la KB y sumar `classpath:db/seed` a `spring.flyway.locations` solo en el perfil `dev`; verificar levantando la app con `dev` que existen los 2 profesionales, 2 sillones, 4 prestaciones, las especialidades (conducto solo para el Dr. Bruno Prueba) y el sillón habitual, y que el perfil `test` no los carga
- [x] 1.4 Extender `ProfesionalEntity`, `SillonEntity` y `PrestacionEntity` en `shared/persistence` (métodos de modificación, activar y desactivar, sillón habitual, especialidades, design D2); verificar que la app arranca con `ddl-auto: validate` contra la base migrada

## 2. Errores y validación de entrada

- [x] 2.1 Crear `CodigoError`, `ConfiguracionException` y `ConfiguracionExceptionHandler` (design D4), que traducen a `ProblemDetail` con `codigo` los errores de negocio, `MethodArgumentNotValidException` (`422 DATOS_INVALIDOS` con `errores`), `TurnoRechazadoException(MARGEN_INVALIDO)` y `DataIntegrityViolationException` de las restricciones únicas; verificar con tests de unidad del manejador que cada código sale con su status

## 3. Profesionales

- [x] 3.1 Implementar `ProfesionalService`, su repositorio y `ProfesionalController` con los endpoints de design D9, DTOs `record` y Bean Validation; verificar que compila y que ninguna respuesta expone una entidad JPA
- [x] 3.2 Escribir los tests de integración de los escenarios "Alta de un profesional", "Matrícula duplicada", "Cambio de matrícula a una que ya existe", "Desactivar un profesional", "Profesional inexistente" y "Profesional sin apellido"; verificar que pasan con `./gradlew integrationTest`

## 4. Sillones y prestaciones

- [x] 4.1 Implementar `SillonService`, su repositorio y `SillonController`; verificar con los tests de integración "Nombre de sillón duplicado" y "Reactivar un sillón"
- [x] 4.2 Implementar `DuracionPrestacion` con sus tests de unidad (0, negativo, 32 y 45 minutos), y `PrestacionService`, su repositorio y `PrestacionController`; verificar con los tests de integración "Prestación con duración válida", "Duración que no es múltiplo de 5" y "Duración cero"
- [x] 4.3 Escribir el test de integración "Turno existente después de cambiar la duración", que inserta un turno de 10:00 a 10:30 por JDBC, cambia la duración por la API y verifica que `turno.fin` no cambió; verificar que pasa

## 5. Especialidades y sillón habitual

- [x] 5.1 Implementar `EspecialidadesService` (reemplazo transaccional del conjunto), el puerto `HabilitacionProfesional` con su adaptador JPA y los endpoints `GET` y `PUT /profesionales/{id}/prestaciones`; verificar con los tests de integración "Definir las especialidades", "Especialidad con una prestación inactiva", "Profesional no habilitado" y "Profesional habilitado"
- [x] 5.2 Implementar `PUT /profesionales/{id}/sillon-habitual`; verificar con los tests de integración "Asignar el sillón habitual", "Sillón habitual inactivo" y "Quitar el sillón habitual"

## 6. Margen

- [x] 6.1 Implementar `MargenService` (que usa `Margen.deMinutos` de C-01), el puerto `MargenVigente` con su adaptador sobre la tabla `configuracion`, y `GET` y `PUT /configuracion/margen`; verificar con los tests de integración "Consultar el margen sin haberlo cambiado", "Aumentar el margen", "Margen menor al mínimo" y "Margen igual al mínimo"
- [x] 6.2 Escribir el test de integración "Margen después de reiniciar", que cambia el margen por la API y lo vuelve a leer con un contexto nuevo de Spring; verificar que pasa

## 7. Verificación integral

- [x] 7.1 Correr `./gradlew test integrationTest` completo y verificar que los 24 escenarios de los specs (19 de `catalogo-consultorio` y 5 de `configuracion-margen`) tienen un test con `@DisplayName` igual a su nombre, todos en verde, y que los tests de C-01 y C-03 siguen pasando
- [x] 7.2 Verificar con `grep` que `configuracion/domain` no importa `org.springframework` ni `jakarta.persistence`, y que ningún controlador devuelve clases de `shared/persistence`
- [x] 7.3 Correr `openspec validate configuracion-consultorio --strict` y verificar que es válido

## Workflow follow-up

- Abrir el PR de `change/C-04-configuracion-consultorio` a `master` avisando a Juan Cruz (`shared/persistence`, `application.yml`) y a Sofia (`build.gradle`). Informar los riesgos de ids `BIGINT` contra `UUID` y del estado `RESERVADO`, y el requisito para C-05 de restringir estos endpoints al rol `ADMIN`.
- Marcar `[x]` en C-04 de `CHANGES.md`.
- Archivar el change con `openspec archive configuracion-consultorio` y verificar los specs `catalogo-consultorio` y `configuracion-margen` en `openspec/specs/`.

## Notas del apply

- **Evidencia**: `./gradlew -p backend test integrationTest` terminó con exit 0 en la corrida `6696ae4d` de active-stack. Pasaron 63 tests: 33 unitarios y 30 de integración, entre ellos los 24 escenarios de C-04, cada uno con su `@DisplayName`, y los 6 de C-03.
- **Desvío del design D11**: los servicios no tienen tests unitarios con dobles en memoria. Los cubren los tests de integración, que entran por la API.
- **Desvío del design D1**: los servicios de `application` usan directamente los repositorios Spring Data de `infrastructure`. Solo `MargenVigente` y `HabilitacionProfesional` son puertos.
- **Hallazgo (se agregó a `build.gradle`)**: en Windows con la hora de Argentina, Java usa el alias `America/Buenos_Aires`, que PostgreSQL 16 rechaza al conectarse. Las tareas de test fijan `user.timezone=America/Argentina/Buenos_Aires`. La app en ejecución (`bootRun` o el jar) va a necesitar lo mismo cuando C-06 o C-14 la levanten.
- **Falla anterior a C-04 (corregida en un commit aparte)**: `TurnosOdontologiaApplicationTest` (Sofia) era `@SpringBootTest` sin `@Tag("integration")`, así que `./gradlew test` necesitaba PostgreSQL desde C-03. Ahora lleva `@Tag("integration")` y `@ActiveProfiles("test")`, y `./gradlew test` corre sin base: 32 tests en la corrida `5fe91a3b`.
- **Riesgo de C-03 (corregido en un commit aparte)**: `RestriccionesTurnoIntegrationTest` (Juan Cruz) hacía `TRUNCATE` sobre la base de `DB_URL` y borraba el seed de desarrollo. Ahora usa `DB_TEST_URL`. En la corrida `43e690c2` pasaron 31 tests de integración y el seed quedó intacto: 7 especialidades antes y después.
