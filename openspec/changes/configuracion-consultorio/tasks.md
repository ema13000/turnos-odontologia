# Tasks

## 1. Base del módulo y migraciones

- [ ] 1.1 Agregar `spring-boot-starter-web` y `spring-boot-starter-validation` a `backend/build.gradle`, y crear el perfil `test` en `application.yml` (con un `DB_URL` propio, recomendado `turnos_test`, y sin la carpeta `db/seed`); verificar que `./gradlew test` sigue en verde
- [ ] 1.2 Crear `db/migration/V2__configuracion_consultorio.sql` (`profesional_prestacion`, `configuracion` con `margen_minutos = 10`, `profesional.sillon_habitual_id` y CHECK de duración múltiplo de 5, design D10); verificar con `./gradlew integrationTest` que Flyway migra y que los tests de C-03 siguen en verde
- [ ] 1.3 Crear `db/seed/V2_1__seed_dev.sql` con los datos ficticios de la KB y sumar `classpath:db/seed` a `spring.flyway.locations` solo en el perfil `dev`; verificar levantando la app con `dev` que existen los 2 profesionales, 2 sillones, 4 prestaciones, las especialidades (conducto solo para el Dr. Bruno Prueba) y el sillón habitual, y que el perfil `test` no los carga
- [ ] 1.4 Extender `ProfesionalEntity`, `SillonEntity` y `PrestacionEntity` en `shared/persistence` (métodos de modificación, activar y desactivar, sillón habitual, especialidades, design D2); verificar que la app arranca con `ddl-auto: validate` contra la base migrada

## 2. Errores y validación de entrada

- [ ] 2.1 Crear `CodigoError`, `ConfiguracionException` y `ConfiguracionExceptionHandler` (design D4), que traducen a `ProblemDetail` con `codigo` los errores de negocio, `MethodArgumentNotValidException` (`422 DATOS_INVALIDOS` con `errores`), `TurnoRechazadoException(MARGEN_INVALIDO)` y `DataIntegrityViolationException` de las restricciones únicas; verificar con tests de unidad del manejador que cada código sale con su status

## 3. Profesionales

- [ ] 3.1 Implementar `ProfesionalService`, su repositorio y `ProfesionalController` con los endpoints de design D9, DTOs `record` y Bean Validation; verificar que compila y que ninguna respuesta expone una entidad JPA
- [ ] 3.2 Escribir los tests de integración de los escenarios "Alta de un profesional", "Matrícula duplicada", "Cambio de matrícula a una que ya existe", "Desactivar un profesional", "Profesional inexistente" y "Profesional sin apellido"; verificar que pasan con `./gradlew integrationTest`

## 4. Sillones y prestaciones

- [ ] 4.1 Implementar `SillonService`, su repositorio y `SillonController`; verificar con los tests de integración "Nombre de sillón duplicado" y "Reactivar un sillón"
- [ ] 4.2 Implementar `DuracionPrestacion` con sus tests de unidad (0, negativo, 32 y 45 minutos), y `PrestacionService`, su repositorio y `PrestacionController`; verificar con los tests de integración "Prestación con duración válida", "Duración que no es múltiplo de 5" y "Duración cero"
- [ ] 4.3 Escribir el test de integración "Turno existente después de cambiar la duración", que inserta un turno de 10:00 a 10:30 por JDBC, cambia la duración por la API y verifica que `turno.fin` no cambió; verificar que pasa

## 5. Especialidades y sillón habitual

- [ ] 5.1 Implementar `EspecialidadesService` (reemplazo transaccional del conjunto), el puerto `HabilitacionProfesional` con su adaptador JPA y los endpoints `GET` y `PUT /profesionales/{id}/prestaciones`; verificar con los tests de integración "Definir las especialidades", "Especialidad con una prestación inactiva", "Profesional no habilitado" y "Profesional habilitado"
- [ ] 5.2 Implementar `PUT /profesionales/{id}/sillon-habitual`; verificar con los tests de integración "Asignar el sillón habitual", "Sillón habitual inactivo" y "Quitar el sillón habitual"

## 6. Margen

- [ ] 6.1 Implementar `MargenService` (que usa `Margen.deMinutos` de C-01), el puerto `MargenVigente` con su adaptador sobre la tabla `configuracion`, y `GET` y `PUT /configuracion/margen`; verificar con los tests de integración "Consultar el margen sin haberlo cambiado", "Aumentar el margen", "Margen menor al mínimo" y "Margen igual al mínimo"
- [ ] 6.2 Escribir el test de integración "Margen después de reiniciar", que cambia el margen por la API y lo vuelve a leer con un contexto nuevo de Spring; verificar que pasa

## 7. Verificación integral

- [ ] 7.1 Correr `./gradlew test integrationTest` completo y verificar que los 24 escenarios de los specs (19 de `catalogo-consultorio` y 5 de `configuracion-margen`) tienen un test con `@DisplayName` igual a su nombre, todos en verde, y que los tests de C-01 y C-03 siguen pasando
- [ ] 7.2 Verificar con `grep` que `configuracion/domain` no importa `org.springframework` ni `jakarta.persistence`, y que ningún controlador devuelve clases de `shared/persistence`
- [ ] 7.3 Correr `openspec validate configuracion-consultorio --strict` y verificar que es válido

## Workflow follow-up

- Abrir el PR de `change/C-04-configuracion-consultorio` a `master` avisando a Juan Cruz (`shared/persistence`, `application.yml`) y a Sofia (`build.gradle`). Informar los riesgos de ids `BIGINT` contra `UUID` y del estado `RESERVADO`, y el requisito para C-05 de restringir estos endpoints al rol `ADMIN`.
- Marcar `[x]` en C-04 de `CHANGES.md`.
- Archivar el change con `openspec archive configuracion-consultorio` y verificar los specs `catalogo-consultorio` y `configuracion-margen` en `openspec/specs/`.
