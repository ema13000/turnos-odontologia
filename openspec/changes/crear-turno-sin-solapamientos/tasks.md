# Tasks

## 1. Scaffolding del backend (merge temprano a `master`)

- [ ] 1.1 Crear `backend/` con el Gradle wrapper 9.x, `settings.gradle` (con `foojay-resolver-convention`) y `build.gradle` (toolchain de Java 21, Spring Boot 3.5.x, `spring-boot-starter` y `spring-boot-starter-test`); verificar que `./gradlew --version` corre y que el wrapper queda commiteado
- [ ] 1.2 Agregar `ar.turnos.odontologia.TurnosOdontologiaApplication` y un test de arranque del contexto; verificar que `./gradlew test` termina en verde
- [ ] 1.3 Agregar al `.gitignore` las carpetas `backend/build/` y `backend/.gradle/`; verificar que `git status` no las muestra después de compilar
- [ ] 1.4 Abrir un PR solo con el grupo 1 y mergearlo a `master`, para desbloquear C-03; verificar que `master` compila con `./gradlew test`

## 2. Dominio de agenda

- [ ] 2.1 Implementar `Intervalo` (semiabierto, con `seSuperponeCon` y `seAcercaA(otro, margen)`) y `EstadoTurno` (con `esActivo()` solo para `RESERVADO` y `CONFIRMADO`); verificar que compila sin dependencias de Spring en `agenda/domain`
- [ ] 2.2 Implementar `CodigoRechazo`, `TurnoRechazadoException` y `Margen` (`deMinutos`, `PREDETERMINADO`, rechazo de valores menores a 10); verificar con `MargenTest` el escenario "Margen menor al mínimo"
- [ ] 2.3 Implementar `Turno` (id, paciente, profesional, sillón, prestación, intervalo, estado) y `Agenda.verificar(nuevo, existentes, margen)` con la prioridad profesional > sillón > margen (design D3); verificar que compila

## 3. Caso de uso Crear turno

- [ ] 3.1 Definir los puertos `TurnoRepository` y `PrestacionRepository`, `CrearTurnoCommand` y `CrearTurno` (con `Clock` y `Margen` inyectados, orden de validación de D7 y ventana `[inicio − M, fin + M)` de D6); verificar que compila
- [ ] 3.2 Crear en `src/test` `TurnoRepositoryEnMemoria` y `PrestacionRepositoryEnMemoria`, y el fixture común (Consulta de 30 min, Dra. A, Dr. B, Sillón 1 y 2, 2026-12-01 en Buenos Aires); verificar que compilan
- [ ] 3.3 Escribir en `CrearTurnoTest` los escenarios "Turno sin conflictos" y "Un turno cancelado no bloquea el horario"; verificar que pasan
- [ ] 3.4 Escribir los escenarios "El profesional ya atiende en otro sillón", "El sillón ya está ocupado por otro profesional" y "Choque por profesional y por sillón a la vez"; verificar que pasan
- [ ] 3.5 Escribir los escenarios "Turno contiguo sin margen", "Turno que respeta el margen exacto", "Turno contiguo sin recursos compartidos" y "Margen mayor al mínimo"; verificar que pasan
- [ ] 3.6 Escribir los escenarios "Inicio anterior al momento actual" e "Inicio igual al momento actual"; verificar que pasan

## 4. Verificación integral

- [ ] 4.1 Correr `./gradlew test` completo y verificar que hay 12 tests de escenario (uno por escenario del spec, con `@DisplayName` igual a su nombre) más el test de arranque, todos en verde
- [ ] 4.2 Verificar que `agenda/domain` no importa nada de `org.springframework` ni de `jakarta.persistence` (búsqueda con `grep`)
- [ ] 4.3 Correr `openspec validate crear-turno-sin-solapamientos --strict` y verificar que es válido

## Workflow follow-up

- Abrir el PR del change completo a `master`, revisarlo y mergearlo.
- Marcar `[x]` en C-01 de `CHANGES.md`.
- Archivar el change con `openspec archive crear-turno-sin-solapamientos` para crear `openspec/specs/agenda-turnos/spec.md`, y verificar el spec archivado.
- Guardar la evidencia del ciclo OPSX para la Etapa 6 (salida de los tests y del `validate`).
