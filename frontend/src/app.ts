import { paginaAgenda } from './pages/agenda.ts';
import { paginaConfiguracion } from './pages/configuracion.ts';
import type { Pagina } from './pages/pagina.ts';
import { paginaReserva } from './pages/reserva.ts';
import { crearLayout } from './shared/ui/layout.ts';
import { crearRouter, type Router } from './shared/ui/router.ts';

export const PAGINAS: readonly Pagina[] = [paginaAgenda, paginaReserva, paginaConfiguracion];

/** Arma la aplicación dentro de `raiz` y muestra la página de la dirección actual. */
export function iniciarApp(raiz: HTMLElement): Router {
  const layout = crearLayout(raiz, PAGINAS);
  const router = crearRouter(PAGINAS, paginaAgenda, (pagina) => {
    pagina.render(layout.contenido);
    layout.mostrar(pagina);
    document.title = `${pagina.titulo} · Turnos odontología`;
  });
  router.actualizar();
  return router;
}
