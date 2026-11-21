import type { Pagina } from '../../pages/pagina.ts';

export interface Router {
  /** Dibuja la página de la dirección actual. */
  actualizar(): void;
  /** Deja de escuchar los cambios de dirección. */
  detener(): void;
}

function rutaDelHash(hash: string): string {
  return hash.replace(/^#\/?/, '');
}

/**
 * Router por hash (`#/agenda`). Con una dirección vacía o desconocida muestra
 * la página inicial y reemplaza la dirección, sin sumar una entrada al historial.
 */
export function crearRouter(
  paginas: readonly Pagina[],
  inicial: Pagina,
  alCambiar: (pagina: Pagina) => void,
): Router {
  const porRuta = new Map(paginas.map((pagina) => [pagina.ruta, pagina]));

  function actualizar(): void {
    const pagina = porRuta.get(rutaDelHash(window.location.hash));
    if (pagina === undefined) {
      history.replaceState(history.state, '', `#/${inicial.ruta}`);
      alCambiar(inicial);
      return;
    }
    alCambiar(pagina);
  }

  window.addEventListener('hashchange', actualizar);
  return {
    actualizar,
    detener: () => window.removeEventListener('hashchange', actualizar),
  };
}
