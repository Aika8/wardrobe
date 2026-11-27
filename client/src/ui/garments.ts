// Garment drawings for the app (ADR-0003): which drawing an item gets, in which fill, as complete SVG markup for
// `SvgXml` of `react-native-svg`. Pure data and functions, no React; tested with `node --test` (test/garments.test.mjs).
// The shapes themselves come only from `garments.data.ts`, generated from docs/design/mockups/garments.py.
import { GARMENTS_DATA } from './garments.data.ts';
import type { ItemType } from '../labels.ts';
import { tokens } from './tokens.ts';

export type GarmentKind = keyof typeof GARMENTS_DATA.garments;

/** Every garment drawing the script exports, in the script's order. */
export const GARMENT_KINDS = Object.keys(GARMENTS_DATA.garments) as GarmentKind[];

/** The drawing of an item of each type, until an item knows its own cut. The one place of this mapping. */
export const DEFAULT_GARMENT: Record<ItemType, GarmentKind> = {
  TOP: 'shirt',
  BOTTOM: 'trousers',
  DRESS: 'dress',
  OUTERWEAR: 'coat',
  SHOES: 'shoes',
  ACCESSORY: 'bag',
};

/** The garment drawing of an item type, or null for a type the contract does not know (nothing is guessed). */
export function garmentKind(type: string): GarmentKind | null {
  return Object.hasOwn(DEFAULT_GARMENT, type) ? DEFAULT_GARMENT[type as ItemType] : null;
}

/** The fill of a drawn item: `item` token by the item's `Colour`; a value the tokens do not know draws as UNKNOWN. */
export function itemFill(colour: string): string {
  return Object.hasOwn(tokens.item, colour) ? tokens.item[colour as keyof typeof tokens.item] : tokens.item.UNKNOWN;
}

const HEX = /^#[0-9A-Fa-f]{6}$/;

function hex(value: string, what: string): string {
  // The value goes into markup: only a plain #RRGGBB colour may, never anything that could open an attribute.
  if (!HEX.test(value)) throw new Error(`${what}: '${value}' is not a #RRGGBB colour`);
  return value;
}

function svg(viewBox: string, body: string): string {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}">${body}</svg>`;
}

/** Complete SVG markup of one garment in the given fill (#RRGGBB), 100 × 100 box. */
export function garmentXml(kind: GarmentKind, fill: string): string {
  if (!Object.hasOwn(GARMENTS_DATA.garments, kind)) throw new Error(`unknown garment: ${String(kind)}`);
  return svg(GARMENTS_DATA.viewBox.garment, GARMENTS_DATA.garments[kind].replaceAll('{colour}', hex(fill, kind)));
}

/** What the doll wears: fills (#RRGGBB) of its shirt and trousers. */
export type DollClothes = { shirt: string; trousers: string };

/** Complete SVG markup of the doll: bare without clothes, else dressed in the two fills; 178 × 360 box. */
export function dollXml(clothes?: DollClothes): string {
  const body = clothes
    ? GARMENTS_DATA.doll.replaceAll('{shirt}', hex(clothes.shirt, 'shirt')).replaceAll('{trousers}', hex(clothes.trousers, 'trousers'))
    : GARMENTS_DATA.doll_bare;
  return svg(GARMENTS_DATA.viewBox.doll, body);
}

const [, , DOLL_W, DOLL_H] = GARMENTS_DATA.viewBox.doll.split(' ').map(Number) as [number, number, number, number];

/** Width and height of the doll's box (its viewBox), for keeping its proportions on screen. */
export const DOLL_BOX = { width: DOLL_W, height: DOLL_H };

/** Where the doll's body parts are, in the doll's box: y of each level, x of the centre line. */
export const DOLL_LANDMARKS = GARMENTS_DATA.doll_landmarks;
