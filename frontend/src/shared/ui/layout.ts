import type { Pagina } from '../../pages/pagina.ts';

export interface Layout {
  readonly contenido: HTMLElement;
  /** Marca el enlace de la página activa y cierra el menú. */
  mostrar(pagina: Pagina): void;
}

const ID_MENU = 'menu-principal';

/** Arma el encabezado con el menú y el área de contenido dentro de `raiz`. */
export function crearLayout(raiz: HTMLElement, paginas: readonly Pagina[]): Layout {
  const encabezado = document.createElement('header');
  encabezado.className = 'encabezado';

  const marca = document.createElement('span');
  marca.className = 'marca';
  marca.textContent = 'Turnos odontología';

  const boton = document.createElement('button');
  boton.type = 'button';
  boton.className = 'menu-boton';
  boton.textContent = 'Menú';
  boton.setAttribute('aria-controls', ID_MENU);
  boton.setAttribute('aria-expanded', 'false');

  const menu = document.createElement('nav');
  menu.id = ID_MENU;
  menu.className = 'menu';
  menu.setAttribute('aria-label', 'Principal');

  const lista = document.createElement('ul');
  const enlaces = paginas.map((pagina) => {
    const enlace = document.createElement('a');
    enlace.href = `#/${pagina.ruta}`;
    enlace.textContent = pagina.titulo;
    enlace.dataset['ruta'] = pagina.ruta;
    const item = document.createElement('li');
    item.append(enlace);
    lista.append(item);
    return enlace;
  });
  menu.append(lista);

  function abrirMenu(abierto: boolean): void {
    menu.classList.toggle('abierto', abierto);
    boton.setAttribute('aria-expanded', String(abierto));
  }

  boton.addEventListener('click', () => abrirMenu(!menu.classList.contains('abierto')));

  encabezado.append(marca, boton, menu);

  const contenido = document.createElement('main');
  contenido.className = 'contenido';

  raiz.replaceChildren(encabezado, contenido);

  return {
    contenido,
    mostrar(pagina) {
      for (const enlace of enlaces) {
        if (enlace.dataset['ruta'] === pagina.ruta) {
          enlace.setAttribute('aria-current', 'page');
        } else {
          enlace.removeAttribute('aria-current');
        }
      }
      abrirMenu(false);
    },
  };
}
