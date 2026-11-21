import './styles.css';
import { iniciarApp } from './app.ts';

const raiz = document.querySelector<HTMLElement>('#app');
if (raiz) {
  iniciarApp(raiz);
}
