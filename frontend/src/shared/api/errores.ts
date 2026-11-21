/** Código que se usa cuando un 409 llega sin la propiedad `codigo`. */
export const CODIGO_CONFLICTO_SIN_CODIGO = 'CONFLICTO_SIN_CODIGO';

/** Mensaje fijo para las fallas que no son de negocio ni de validación. */
export const MENSAJE_ERROR_GENERICO =
  'No pudimos completar la operación. Probá de nuevo en unos minutos.';

/** Base de todos los errores que entrega el cliente de API. */
export class ErrorApi extends Error {
  constructor(mensaje: string) {
    super(mensaje);
    this.name = new.target.name;
  }
}

/** Respuesta 409: una regla de negocio rechazó el pedido. */
export class ErrorNegocio extends ErrorApi {
  readonly codigo: string;
  readonly detalle: string;

  constructor(codigo: string, detalle: string) {
    super(detalle || codigo);
    this.codigo = codigo;
    this.detalle = detalle;
  }
}

/** Respuesta 422: los datos enviados no son válidos. */
export class ErrorValidacion extends ErrorApi {
  readonly codigo: string | null;
  readonly detalle: string;

  constructor(detalle: string, codigo: string | null) {
    super(detalle || 'Los datos enviados no son válidos.');
    this.codigo = codigo;
    this.detalle = detalle;
  }
}

/** Cualquier otra falla; `estado` es null si no hubo respuesta del servidor. */
export class ErrorGenerico extends ErrorApi {
  readonly estado: number | null;

  constructor(estado: number | null) {
    super(MENSAJE_ERROR_GENERICO);
    this.estado = estado;
  }
}

/** Campos del ProblemDetail (RFC 9457) que usa el frontend. */
export interface ProblemDetail {
  readonly detail: string;
  readonly codigo: string | null;
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null && !Array.isArray(valor);
}

function textoNoVacio(valor: unknown): string | null {
  return typeof valor === 'string' && valor.trim() !== '' ? valor : null;
}

/** Interpreta un cuerpo de error desconocido; nunca falla. */
export function leerProblemDetail(cuerpo: unknown): ProblemDetail {
  if (!esObjeto(cuerpo)) {
    return { detail: '', codigo: null };
  }
  return {
    detail: textoNoVacio(cuerpo['detail']) ?? '',
    codigo: textoNoVacio(cuerpo['codigo']),
  };
}
