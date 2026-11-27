// `POST /avatar/dress` (ADR-0002: no React here; tested with `node --test` and a fake `fetch`). The server places
// and fits the clothes; this file only asks and checks that the answer has the contract's shape.
import type { DressedAvatar, DressRequest, Layer } from './types.ts';

type FetchFn = (url: string, init?: RequestInit) => Promise<Response>;

const LANDMARKS = ['head', 'neck', 'shoulders', 'chest', 'waist', 'hips', 'crotch', 'knee', 'ankle', 'floor'] as const;
const WIDTHS = ['shoulders', 'chest', 'waist', 'hips'] as const;

function obj(v: unknown, what: string): Record<string, unknown> {
  if (typeof v !== 'object' || v === null || Array.isArray(v)) throw new Error(`${what} is not an object`);
  return v as Record<string, unknown>;
}

function int(v: unknown, what: string): number {
  if (!Number.isInteger(v)) throw new Error(`${what} is not an integer`);
  return v as number;
}

function str(v: unknown, what: string): string {
  if (typeof v !== 'string') throw new Error(`${what} is not a string`);
  return v;
}

/** The answer as `DressedAvatar`, or throws naming the first field that does not match the contract. */
export function parseDressed(body: unknown): DressedAvatar {
  const b = obj(body, 'answer');
  const f = obj(b.figure, 'figure');
  const lm = obj(f.landmarks, 'figure.landmarks');
  const hw = obj(f.halfWidths, 'figure.halfWidths');
  const landmarks = Object.fromEntries(LANDMARKS.map((k) => [k, int(lm[k], `landmarks.${k}`)]));
  const halfWidths = Object.fromEntries(WIDTHS.map((k) => [k, int(hw[k], `halfWidths.${k}`)]));
  if (!Array.isArray(b.layers)) throw new Error('layers is not an array');
  const layers = b.layers.map((raw, i): Layer => {
    const l = obj(raw, `layers[${i}]`);
    if (!Array.isArray(l.fit)) throw new Error(`layers[${i}].fit is not an array`);
    const layer: Layer = {
      itemId: str(l.itemId, `layers[${i}].itemId`),
      type: str(l.type, `layers[${i}].type`),
      fit: l.fit.map((n, j) => {
        const note = obj(n, `layers[${i}].fit[${j}]`);
        return { kind: str(note.kind, `fit[${j}].kind`), part: str(note.part, `fit[${j}].part`) };
      }),
    };
    if (l.shape !== undefined) {
      const s = obj(l.shape, `layers[${i}].shape`);
      layer.shape = {
        top: int(s.top, 'shape.top'),
        bottom: int(s.bottom, 'shape.bottom'),
        topHalfWidth: int(s.topHalfWidth, 'shape.topHalfWidth'),
        bottomHalfWidth: int(s.bottomHalfWidth, 'shape.bottomHalfWidth'),
        clamped: s.clamped === true,
        ...(s.sleeveEnd === undefined ? {} : { sleeveEnd: int(s.sleeveEnd, 'shape.sleeveEnd') }),
      };
    }
    return layer;
  });
  return { figure: { landmarks, halfWidths } as DressedAvatar['figure'], layers };
}

/** `POST /avatar/dress`. Throws an `Error` with the server's `Error.message` when it refuses (400, 404, 409). */
export async function dress(baseUrl: string, request: DressRequest, fetchFn: FetchFn = fetch): Promise<DressedAvatar> {
  const r = await fetchFn(`${baseUrl}/avatar/dress`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!r.ok) {
    let message = `HTTP ${r.status}`;
    try {
      const e = (await r.json()) as { message?: unknown } | null;
      if (typeof e?.message === 'string' && e.message.length > 0) message = e.message;
    } catch {
      // not JSON: keep the status line
    }
    throw new Error(message);
  }
  return parseDressed(await r.json());
}
