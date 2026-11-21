/** Una página de la aplicación: su dirección, el texto del menú y cómo se dibuja. */
export interface Pagina {
  readonly ruta: string;
  readonly titulo: string;
  render(contenedor: HTMLElement): void;
}

/** Página que por ahora muestra solo su título (C-09 y C-12 suman el contenido). */
export function paginaConTitulo(ruta: string, titulo: string): Pagina {
  return {
    ruta,
    titulo,
    render(contenedor) {
      const encabezado = document.createElement('h1');
      encabezado.textContent = titulo;
      encabezado.tabIndex = -1;
      contenedor.replaceChildren(encabezado);
    },
  };
}
