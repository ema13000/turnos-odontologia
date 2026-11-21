# Verificación de fuentes (consigna, Etapa 2)

Cada integrante **abre personalmente** su fuente en el navegador y registra lo que encontró. **No uses IA para esta parte**: la consigna evalúa justamente que lo hayan comprobado ustedes.

## Asignación

| # | Tipo | URL | Qué afirma el informe | Responsable | Archivo |
|---|---|---|---|---|---|
| 1 | Sitio oficial | https://bilog.com.ar/ | +1.500 clínicas; reserva y cancelación online; recordatorios por WhatsApp; sin precios publicados | Emanuel | `ema.md` |
| 2 | Página de precios | https://dentalsoft.com.ar/ (sección planes) | Plan Gratis de 50 turnos/mes; Gestión $30.000 y Pro $60.000 por profesional; costo de Meta de USD 0,026 por mensaje | Juan Sebastián | `juan-sebastian.md` |
| 3 | Centro de ayuda | https://ayuda.softwaredentalink.com/es/collections/19196578-modulo-exclusivo-para-argentina | Módulo para Argentina: obras sociales, conciliaciones, receta electrónica | Juan Cruz | `juan-cruz.md` |
| 4 | Video oficial | https://odonthia.com/ (video "Mirá Odonthia por dentro") | Odontograma FDI con dientes temporales; plan gratuito permanente | Enzo | `enzo.md` |
| 5 | Demo pública | https://vectora.ar/pages/vecdent/ (demo sin registro) | Periodontograma de 6 puntos; portal del paciente; ARS 30.000, 50.000 y 80.000 | Sofia | `sofia.md` |
| 6 (opcional) | Sitio oficial | https://dentalcore.app/ | Precio ARS 75.000 contra USD 50 (inconsistencia) | _(libre)_ | |

Reglas:

- **No crees cuentas ni dejes datos personales** en los sitios. Solo páginas, videos y demos públicas.
- Si algo no coincide, escribilo tal cual. Encontrar errores **suma puntos**, no resta.

## Plantilla (copiala en tu archivo `docs/discovery/verificacion/<tu-nombre>.md`)

```markdown
# Verificación: <Producto>

- **Responsable**: <Nombre>
- **URL**: <url>
- **Tipo de fuente**: <sitio oficial | precios | centro de ayuda | video | demo>
- **Fecha y hora de consulta**: AAAA-MM-DD HH:MM

## Qué afirma el informe
<copiar de la tabla>

## Qué encontré
- Afirmación 1: ✅ coincide / ❌ no coincide / ⚠️ parcial. Detalle: ...
- Afirmación 2: ...

## Corrección propuesta
<"ninguna" o qué hay que cambiar en el informe>

## Evidencia
<captura en docs/discovery/verificacion/img/<tu-nombre>-1.png (opcional)>
```

## Cómo entregarlo (Git)

1. `git clone https://github.com/sofi0802/turnos-odontologia.git`
2. `git checkout -b verificacion/<tu-nombre>`
3. Creá tu archivo con la plantilla y hacé commit **con tu nombre y email de GitHub**.
4. `git push -u origin verificacion/<tu-nombre>` y abrí un Pull Request a `master`.
5. Sofia revisa, mergea y consolida los resultados en la tabla del informe.

Cada uno escribe **su propio archivo** para que no haya conflictos de merge.
