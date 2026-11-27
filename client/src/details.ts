// Pure logic of the item details screen (ADR-0002: no React here, tested with `node --test`).
// Every word comes from `labels.ts`; this file only picks the rows and their order.
import type { Item } from './items.ts';
import { FORMALITY_HINT, label, SEASON_LABEL, type Formality, type Season } from './labels.ts';

export type DetailRow = { label: string; value: string; hint?: string };

/** Seasons in calendar order (winter first), the order of the enum in `api/openapi.yaml`. */
export const SEASON_ORDER: readonly Season[] = ['WINTER', 'SPRING', 'SUMMER', 'AUTUMN'];

function seasonsValue(seasons: readonly string[]): string {
  const known = SEASON_ORDER.filter((s) => seasons.includes(s));
  const unknown = seasons.filter((s) => !(s in SEASON_LABEL));
  return [...known.map((s) => label('seasons', s)), ...unknown].join(', ');
}

/**
 * The rows of an item's details, in Russian: type, colour, pattern, fabric, seasons, formality.
 * The name is the screen title and the status is the badge, so neither is a row; `id` is never shown.
 * A value the tables lack is shown as it came (`label`), never guessed.
 */
export function describeItem(item: Item): DetailRow[] {
  const hint = FORMALITY_HINT[item.formality as Formality];
  return [
    { label: 'Тип', value: label('type', item.type) },
    { label: 'Цвет', value: label('colour', item.colour) },
    { label: 'Узор', value: label('pattern', item.pattern) },
    { label: 'Ткань', value: label('fabric', item.fabric) },
    { label: 'Сезоны', value: seasonsValue(item.seasons) },
    { label: 'Стиль', value: label('formality', item.formality), ...(hint ? { hint } : {}) },
  ];
}
