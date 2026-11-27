// Fitting the template doll onto the figure the server computed (POST /avatar/dress; WR-027). Draft — вид проверяет Aika.
// Pure functions, no React; tested with `node --test` (test/avatar.test.mjs). No fit geometry here: where a garment
// falls and how it fits are the server's (domain `Placement`, `Fit`); this file only moves drawn points from the
// template's body lines to the figure's, piecewise-linearly, and rounds them to whole pixels.
import { GARMENTS_DATA } from '../ui/garments.data.ts';
import { DOLL_BOX, DOLL_LANDMARKS, itemFill } from '../ui/garments.ts';
import { tokens } from '../ui/tokens.ts';
import type { DressedAvatar, FigureLandmarks, FitPart, Layer } from './types.ts';

/** The template's line for each of the figure's lines (the template says head_top/chin where the figure says head/neck). */
const LEVELS: ReadonlyArray<[keyof FigureLandmarks, keyof typeof DOLL_LANDMARKS]> = [
  ['head', 'head_top'],
  ['neck', 'chin'],
  ['shoulders', 'shoulders'],
  ['chest', 'chest'],
  ['waist', 'waist'],
  ['hips', 'hips'],
  ['crotch', 'crotch'],
  ['knee', 'knee'],
  ['ankle', 'ankle'],
  ['floor', 'floor'],
];

const WIDTHS = ['shoulders', 'chest', 'waist', 'hips'] as const;
type WidthLine = (typeof WIDTHS)[number];

export type Point = { x: number; y: number };

/** The figure on screen: pixel box, centre line, and the pairs (template y → pixel y / width ratio) to move points by. */
export type Bands = {
  width: number;
  height: number;
  centre: number;
  /** Pixels per template unit of the box (the doll's box scaled to `height`). */
  scale: number;
  /** Template y → pixel y at each body line, top to bottom. */
  levels: Array<[number, number]>;
  /** Template y → how much wider than the template the figure is there (1 = as drawn). */
  ratios: Array<[number, number]>;
  /** The figure's lines in pixels. */
  y: Record<keyof FigureLandmarks, number>;
  /** The figure's half-widths in pixels. */
  halfWidth: Record<WidthLine, number>;
  /** Pixels per mm of the figure. */
  pxPerMm: number;
  /** mm → pixels along the figure's height (head at `y.head`). */
  mm: (value: number) => number;
};

/** Points of the template body outline (first path of `doll_bare`), curves flattened; absolute M/L/C/Z only. */
function bodyOutline(): Point[] {
  const d = /<path d="([^"]*)"/.exec(GARMENTS_DATA.doll_bare)?.[1];
  if (d === undefined) throw new Error('doll_bare has no body path');
  const toks = d.match(/[MLCZ]|-?\d+(?:\.\d+)?/g) ?? [];
  const pts: Point[] = [];
  let cur: Point = { x: 0, y: 0 };
  let cmd = '';
  let i = 0;
  const num = () => Number(toks[i++]);
  while (i < toks.length) {
    if (/[MLCZ]/.test(toks[i]!)) cmd = toks[i++]!;
    if (cmd === 'Z') continue;
    if (cmd === 'M' || cmd === 'L') {
      cur = { x: num(), y: num() };
      pts.push(cur);
    } else if (cmd === 'C') {
      const c1 = { x: num(), y: num() };
      const c2 = { x: num(), y: num() };
      const p = { x: num(), y: num() };
      for (let s = 1; s <= 32; s++) {
        const t = s / 32;
        const u = 1 - t;
        pts.push({
          x: u * u * u * cur.x + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * p.x,
          y: u * u * u * cur.y + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * p.y,
        });
      }
      cur = p;
    }
  }
  return pts;
}

/** Half-width of the template's body at y: from the centre line to the outermost left edge of the outline. */
function templateHalfWidthAt(outline: Point[], y: number): number {
  let best = Number.NaN;
  for (let k = 1; k < outline.length; k++) {
    const a = outline[k - 1]!;
    const b = outline[k]!;
    if ((a.y - y) * (b.y - y) > 0 || a.y === b.y) continue;
    const x = a.x + ((y - a.y) / (b.y - a.y)) * (b.x - a.x);
    if (x < DOLL_LANDMARKS.centre_x && !(DOLL_LANDMARKS.centre_x - x <= best)) best = DOLL_LANDMARKS.centre_x - x;
  }
  if (Number.isNaN(best)) throw new Error(`the template body has no edge at y=${y}`);
  return best;
}

/** The template doll's half-widths at its shoulder, chest, waist and hip lines, read off its own outline. */
export const TEMPLATE_HALF_WIDTH: Record<WidthLine, number> = (() => {
  const outline = bodyOutline();
  return Object.fromEntries(WIDTHS.map((w) => [w, templateHalfWidthAt(outline, DOLL_LANDMARKS[w])])) as Record<
    WidthLine,
    number
  >;
})();

/** Piecewise-linear through sorted (from → to) pairs; outside them the end piece goes on (or holds, `hold`). */
function through(pairs: ReadonlyArray<[number, number]>, v: number, hold = false): number {
  const n = pairs.length;
  let k = 1;
  while (k < n - 1 && v > pairs[k]![0]) k++;
  const [x0, y0] = pairs[k - 1]!;
  const [x1, y1] = pairs[k]!;
  if (hold && v <= pairs[0]![0]) return pairs[0]![1];
  if (hold && v >= pairs[n - 1]![0]) return pairs[n - 1]![1];
  return x1 === x0 ? y0 : y0 + ((v - x0) / (x1 - x0)) * (y1 - y0);
}

/**
 * The server's figure (mm) as pixel bands in a box `heightPx` high, with the doll's proportions: head and floor sit
 * where the template's head and floor sit in its box, so every figure is drawn at the same height.
 */
export function bands(answer: Pick<DressedAvatar, 'figure'>, heightPx: number): Bands {
  const { landmarks, halfWidths } = answer.figure;
  const scale = heightPx / DOLL_BOX.height;
  const headPx = Math.round(DOLL_LANDMARKS.head_top * scale);
  const floorPx = Math.round(DOLL_LANDMARKS.floor * scale);
  const span = landmarks.floor - landmarks.head;
  if (!(span > 0)) throw new Error('the figure has no height');
  const pxPerMm = (floorPx - headPx) / span;
  const mm = (value: number) => Math.round(headPx + (value - landmarks.head) * pxPerMm);
  const y = Object.fromEntries(LEVELS.map(([f]) => [f, mm(landmarks[f])])) as Record<keyof FigureLandmarks, number>;
  const halfWidth = Object.fromEntries(WIDTHS.map((w) => [w, Math.round(halfWidths[w] * pxPerMm)])) as Record<
    WidthLine,
    number
  >;
  const levels = LEVELS.map(([f, t]): [number, number] => [DOLL_LANDMARKS[t], y[f]]);
  // Above the shoulders the head and neck keep the template's width; from the hips down the legs follow the hips.
  const ratios: Array<[number, number]> = [
    [DOLL_LANDMARKS.head_top, 1],
    [DOLL_LANDMARKS.chin, 1],
    ...WIDTHS.map((w): [number, number] => [DOLL_LANDMARKS[w], halfWidth[w] / (TEMPLATE_HALF_WIDTH[w] * scale)]),
  ];
  return {
    width: Math.round(DOLL_BOX.width * scale),
    height: Math.round(heightPx),
    centre: Math.round(DOLL_LANDMARKS.centre_x * scale),
    scale,
    levels,
    ratios,
    y,
    halfWidth,
    pxPerMm,
    mm,
  };
}

/** A template point (doll box units) moved onto the figure, in whole pixels. */
export function place(point: Point, b: Bands): Point {
  const k = through(b.ratios, point.y, true);
  return {
    x: Math.round(b.centre + (point.x - DOLL_LANDMARKS.centre_x) * b.scale * k),
    y: Math.round(through(b.levels, point.y)),
  };
}

const fmt = (n: number) => String(n);

/** Template SVG markup (absolute M/L/C/Z paths, circles, ellipses) with every point moved onto the figure. */
export function warpXml(markup: string, b: Bands): string {
  const pathOut = markup.replace(/ d="([^"]*)"/g, (_, d: string) => {
    const toks = d.match(/[A-Za-z]|-?\d+(?:\.\d+)?/g) ?? [];
    const out: string[] = [];
    for (let i = 0; i < toks.length; i++) {
      const t = toks[i]!;
      if (/[A-Za-z]/.test(t)) {
        if (!/[MLCZ]/.test(t)) throw new Error(`path command '${t}' is not supported`);
        out.push(t);
        continue;
      }
      const p = place({ x: Number(t), y: Number(toks[++i]) }, b);
      out.push(`${fmt(p.x)} ${fmt(p.y)}`);
    }
    return ` d="${out.join(' ')}"`;
  });
  return pathOut.replace(/<(circle|ellipse)([^>]*)\/>/g, (_, tag: string, attrs: string) => {
    const get = (name: string) => Number(new RegExp(` ${name}="([^"]*)"`).exec(attrs)?.[1]);
    const c = { x: get('cx'), y: get('cy') };
    const at = place(c, b);
    const rx = tag === 'circle' ? get('r') : get('rx');
    const ry = tag === 'circle' ? get('r') : get('ry');
    const nrx = Math.max(1, Math.abs(place({ x: c.x + rx, y: c.y }, b).x - at.x));
    const nry = Math.max(1, Math.abs(place({ x: c.x, y: c.y + ry }, b).y - at.y));
    const set = (s: string, name: string, v: number) => s.replace(new RegExp(` ${name}="[^"]*"`), ` ${name}="${fmt(v)}"`);
    let a = set(set(attrs, 'cx', at.x), 'cy', at.y);
    a = tag === 'circle' ? set(a, 'r', Math.min(nrx, nry)) : set(set(a, 'rx', nrx), 'ry', nry);
    return `<${tag}${a}/>`;
  });
}

/** The template's clothes, split off the dressed doll: what is drawn for the trousers and what for the shirt. */
export const TEMPLATE_CLOTHES = (() => {
  const { doll, doll_bare } = GARMENTS_DATA;
  if (!doll.startsWith(doll_bare)) throw new Error('the dressed doll does not start with the bare doll');
  const rest = doll.slice(doll_bare.length);
  const elements = rest.match(/<[^>]+>/g) ?? [];
  const firstShirt = elements.findIndex((e) => e.includes('{shirt}'));
  return { trousers: elements.slice(0, firstShirt).join(''), shirt: elements.slice(firstShirt).join('') };
})();

/** Which template garment a layer of this type is drawn as; other types are listed, not drawn (DECISIONS.md, WR-027). */
export function drawnAs(type: string): 'shirt' | 'trousers' | null {
  return type === 'TOP' ? 'shirt' : type === 'BOTTOM' ? 'trousers' : null;
}

export type Marker = { x: number; y: number; n: number };

/**
 * Where a fit note's marker sits: at the end of the garment edge it is about, from the server's shape — the sleeve
 * end (left, at the garment's top half-width), the hem (right, at its bottom half-width), or the side of the body at
 * the line of a girth. Null when the shape does not say where (no shape, or no sleeve end).
 */
export function markerAt(layer: Layer, part: FitPart | string, b: Bands): Point | null {
  const s = layer.shape;
  if (s === undefined) return null;
  switch (part) {
    case 'SLEEVE':
      return s.sleeveEnd === undefined ? null : { x: b.centre - Math.round(s.topHalfWidth * b.pxPerMm), y: b.mm(s.sleeveEnd) };
    case 'INSEAM':
      return { x: b.centre + Math.round(s.bottomHalfWidth * b.pxPerMm), y: b.mm(s.bottom) };
    case 'CHEST':
      return { x: b.centre + b.halfWidth.chest, y: b.y.chest };
    case 'WAIST':
      return { x: b.centre + b.halfWidth.waist, y: b.y.waist };
    case 'HIPS':
      return { x: b.centre + b.halfWidth.hips, y: b.y.hips };
    default:
      return null;
  }
}

/** A drawn layer's colour, by item id: the item's `Colour` from the API (e.g. 'NAVY'). */
export type Colours = Record<string, string>;

/** The numbered markers of the fit notes of the drawn layers; numbers are the layer's place in `layers` (from 1). */
export function markers(answer: DressedAvatar, b: Bands): Marker[] {
  const out: Marker[] = [];
  answer.layers.forEach((layer, i) => {
    if (drawnAs(layer.type) === null) return;
    for (const note of layer.fit) {
      const at = markerAt(layer, note.part, b);
      if (at !== null) out.push({ ...at, n: i + 1 });
    }
  });
  return out;
}

/**
 * Complete SVG markup of the dressed doll in a box `b.width` × `b.height`: the bare doll, then the drawn layers in
 * the answer's order (bottom layer first), then the fit markers. Every point is moved onto the figure.
 */
export function avatarXml(answer: DressedAvatar, colours: Colours, b: Bands): string {
  const parts: string[] = [GARMENTS_DATA.doll_bare];
  for (const layer of answer.layers) {
    const kind = drawnAs(layer.type);
    if (kind === null) continue;
    parts.push(TEMPLATE_CLOTHES[kind].replaceAll(`{${kind}}`, itemFill(colours[layer.itemId] ?? 'UNKNOWN')));
  }
  const r = Math.max(1, Math.round(6.5 * b.scale));
  const marks = markers(answer, b).map(
    (m) =>
      `<circle cx="${m.x}" cy="${m.y}" r="${r}" fill="${tokens.colour.mustard}" stroke="${tokens.colour.card}" stroke-width="1.5"/>` +
      `<text x="${m.x}" y="${m.y + Math.round(r / 2)}" font-size="${Math.round(r * 1.4)}" font-weight="800" text-anchor="middle" fill="${tokens.colour['on-accent']}">${m.n}</text>`,
  );
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${b.width} ${b.height}">${warpXml(parts.join(''), b)}${marks.join('')}</svg>`;
}
