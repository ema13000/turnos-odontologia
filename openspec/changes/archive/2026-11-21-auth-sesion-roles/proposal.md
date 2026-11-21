# Proposal

## Why

Hoy la API no tiene autenticación ni autorización: cualquiera que llegue al backend podría leer o modificar la agenda. Antes de exponer turnos (C-06), la reserva online (C-12) y la ficha clínica (C-13), el sistema necesita saber **quién** hace cada pedido y **qué rol** tiene, y garantizar que un paciente solo vea sus propios datos (RN-RO-08, Ley 25.326). Este change es C-05, del camino crítico del MVP (`C-01 → C-03 → C-05 → C-06 …`), con governance **CRÍTICO**: no se escribe código hasta que la líder apruebe estos artefactos.

## What Changes

- **Spring Security con sesión del lado del servidor y cookie** (DD-10): cookie de sesión `HttpOnly`, `SameSite=Lax` y `Secure` configurable por entorno; vencimiento por inactividad configurable (`SESSION_TIMEOUT`). **Sin JWT.**
- **CSRF activo** con la convención que ya asume el frontend (C-02): cookie `XSRF-TOKEN` legible y encabezado `X-XSRF-TOKEN`. El token se renueva al iniciar sesión.
- **Contraseñas con BCrypt**; nunca se devuelven, ni se guardan en claro, ni se loguean.
- **Usuario y roles**: tabla `usuario` con los roles `ADMIN`, `RECEPCIONISTA`, `ODONTOLOGO` y `PACIENTE` (**migración V3**). Un `PACIENTE` está vinculado a exactamente un paciente.
- **Endpoints**:
  - `POST /api/v1/public/login` y `POST /api/v1/logout` (el `POST /logout` de `CHANGES.md`, bajo el prefijo `/api/v1`).
  - `GET /api/v1/public/sesion`: estado de la sesión (usuario y rol, o sin sesión) y emisión del token CSRF para el frontend.
  - `POST /api/v1/public/pacientes/registro`: registro único del paciente (US-024), que crea el paciente y su cuenta.
  - `GET /api/v1/paciente/perfil`: datos propios del paciente con sesión. Es el primer endpoint de `/paciente/**` y prueba el aislamiento entre pacientes.
- **Reglas de acceso denegadas por defecto**: `/api/v1/public/**` abierto; `/api/v1/paciente/**` solo `PACIENTE` y solo sus datos (el paciente se identifica **siempre por la sesión**, nunca por un parámetro del pedido); el resto de `/api/v1/**` solo para el personal, con el detalle por rol de la matriz RBAC en cada endpoint (seguridad por método); cualquier otra ruta, denegada.
- **Errores estables** en `ProblemDetail` con la propiedad `codigo`: `401` (`NO_AUTENTICADO`, `CREDENCIALES_INVALIDAS`), `403` (`ACCESO_DENEGADO`, `CSRF_INVALIDO`), `409` (`DNI_YA_REGISTRADO`, `EMAIL_YA_REGISTRADO`) y `422` (`DATOS_INVALIDOS`, con el detalle por campo).
- **CORS** solo para los orígenes configurados (el servidor de Vite en desarrollo), con credenciales y nunca `*`.
- **Alta del primer `ADMIN`** desde variables de entorno al arrancar, si todavía no hay ninguno (*propuesta, a confirmar*).
- **Un test por escenario** de los specs: login correcto e incorrecto, `401`/`403` por rol, CSRF, y un paciente que no accede a los datos de otro.

### Decisiones propuestas, a confirmar por la líder

Están reflejadas en los specs y el diseño como **propuesta**. Si alguna cambia, se actualizan los artefactos antes de implementar.

1. **Usuario del paciente = su email** (en minúsculas). El personal usa un `username` libre (`admin`, `recepcion`, `ana.ejemplo`). Alternativa: DNI como usuario (más adivinable y es un dato personal).
2. **Política de contraseña**: entre 8 y 72 caracteres, sin reglas de composición (criterio NIST 800-63B; 72 es el límite de BCrypt).
3. **Primer `ADMIN`** creado al arrancar desde `APP_ADMIN_USERNAME` y `APP_ADMIN_PASSWORD`, solo si no existe ningún `ADMIN`. Nunca un hash fijo en una migración (regla dura 4).
4. **Registro sin inicio de sesión automático**: responde `201` y el frontend llama al login.
5. **DNI ya cargado por el consultorio**: el registro responde `409 DNI_YA_REGISTRADO` aunque ese paciente no tenga cuenta. Vincular una cuenta a un paciente existente exige una verificación que este change no resuelve.
6. **Vencimiento de la sesión**: 30 minutos de inactividad por defecto (`SESSION_TIMEOUT`, como ya figura en la KB).
7. **Login con credenciales incorrectas, usuario inexistente o usuario inactivo**: la misma respuesta `401 CREDENCIALES_INVALIDAS`, para no revelar qué usuarios existen.

**Fuera de alcance**:
- **ABM de usuarios del personal** (`ADMIN` crea, edita y desactiva recepcionistas y odontólogos): la matriz RBAC lo pide, pero **ningún change de `CHANGES.md` lo tiene asignado**. Hay que decidir dónde va (pregunta para la líder).
- Límite de intentos de login (`429`) y bloqueo de cuenta: la skill de seguridad lo recomienda, pero suma una dependencia o un componente nuevo. Queda como pregunta abierta; si se aprueba, agrega un requisito.
- Recuperación y cambio de contraseña, verificación del email, segundo factor.
- Cerrar las sesiones abiertas al desactivar un usuario, y sesiones compartidas entre varias instancias (DD-10, trade-off aceptado).
- Usuarios de demo del seed (`recepcion`, `ana.ejemplo`, `paciente.demo`): dependen del seed de profesionales de C-04 (V2). Los tests crean sus propios usuarios ficticios.
- Consentimiento de comunicaciones y `tiene_whatsapp` del paciente (RN-NO-03): columnas que hoy no existen en V1; las agrega el change de notificaciones.
- Reservas, turnos y cancelaciones del paciente (`/paciente/reservas`, `/paciente/turnos`): C-12. Endpoints del personal (turnos, configuración, ficha): C-04, C-06, C-13; cada uno declara su regla de rol con la seguridad por método que deja este change.
- Webhook de WhatsApp (`/api/v1/webhooks/**`): queda denegado por defecto hasta que su change lo abra con validación de firma.
- Pantallas de login y registro del frontend: changes de frontend (Enzo).

## Capabilities

### New Capabilities

- `seguridad-autenticacion`: cómo una persona inicia y cierra sesión, cómo viaja la sesión (cookie) y cómo se protege contra CSRF, y cómo se crea el primer administrador.
- `seguridad-autorizacion`: qué rutas y operaciones puede usar cada rol, la denegación por defecto, las respuestas `401`/`403`, el aislamiento de los datos de cada paciente y los orígenes permitidos para el frontend. Los changes que agregan endpoints suman acá sus reglas de rol.
- `cuentas-pacientes`: el registro único del paciente con su cuenta (US-024) y las reglas de unicidad y validación de sus datos.

### Modified Capabilities

Ninguna: `agenda-turnos` no cambia.

## Impact

- **Código nuevo** (dueño: Juan Sebastián): `backend/.../shared/security/` y `backend/.../usuarios/`.
- **Migración V3** (reservada para este change): tabla `usuario`. No se toca V1 ni V2.
- **Módulos ajenos que se tocan, con aviso en el PR**:
  - `backend/build.gradle` (Sofia, scaffolding): starters `web`, `validation`, `security` y `spring-security-test`. Puede chocar con C-04, que también agrega `web`.
  - `backend/.../shared/persistence/PacienteEntity` (Juan Cruz): hoy no permite cargar email ni teléfono, y el registro los necesita.
  - `.env.example` (Juan Cruz): nuevas variables de ejemplo, sin valores reales.
  - `application.yml`: propiedades de sesión y CORS.
- **Contratos con otros changes**: el formato CSRF que ya implementó C-02 (Enzo); la propiedad `codigo` del `ProblemDetail`, que C-06 (Sofia) adopta en su manejador global. Este change usa un manejador limitado a sus propios paquetes para no pisar el de C-06.
- **Diferencias con la KB que se señalan**: `08_arquitectura_propuesta.md` §Seguridad dice que el paciente no tiene cuenta y solo usa tokens en `/public/**`, lo que contradice RN-RO-08 y el scope de C-05 (que sí le dan cuenta). Este change sigue RN-RO-08; la KB debería corregirse. La KB modela `Usuario.id` como UUID, pero V1 usa `BIGINT` identidad: se mantiene `BIGINT` por coherencia.
