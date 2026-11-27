// What the avatar screen asks the server for and how it words the answer (WR-027; pure, tested with `node --test`).
// Draft — вид проверяет Aika. Fit is the server's: this file only picks the items and puts its notes into words.
import type { Item } from '../items.ts';
import { TYPE_LABEL, type ItemType } from '../labels.ts';
import { drawnAs } from './fit.ts';
import type { DressRequest, FitNote, Layer, Measurements } from './types.ts';

/** The one invented figure of version 1 (the mockup's: 168 cm; chest 88, waist 70, hips 96, arm 58, inseam 78). */
export const INVENTED_FIGURE: Measurements = {
  height: 1680,
  shoulders: 380,
  chest: 880,
  waist: 700,
  hips: 960,
  arm: 580,
  inseam: 780,
};

/** The measurements shown beside the doll, in the mockup's order: a word and centimetres for the person. */
export function measureRows(m: Measurements): Array<{ label: string; value: string }> {
  const cm = (mm: number) => `${Math.round(mm / 10)} см`;
  return [
    { label: 'Рост', value: cm(m.height) },
    { label: 'Плечи', value: cm(m.shoulders) },
    { label: 'Грудь', value: cm(m.chest) },
    { label: 'Талия', value: cm(m.waist) },
    { label: 'Бёдра', value: cm(m.hips) },
    { label: 'Рука', value: cm(m.arm) },
    { label: 'Шаг', value: cm(m.inseam) },
  ];
}

/**
 * The outfit to put on: the items whose ids the route names (comma-separated, in that order), else the first top and
 * the first bottom of the wardrobe — clean ones first. No sizes: version 1 keeps none, so the server knows no fit.
 */
export function outfitRequest(items: Item[], param?: string): DressRequest {
  let picked: Item[];
  if (param !== undefined && param.length > 0) {
    const byId = new Map(items.map((i) => [i.id, i]));
    picked = param.split(',').flatMap((id) => (byId.has(id) ? [byId.get(id)!] : []));
  } else {
    const first = (type: string) =>
      items.find((i) => i.type === type && i.status === 'CLEAN') ?? items.find((i) => i.type === type);
    picked = [first('TOP'), first('BOTTOM')].filter((i): i is Item => i !== undefined);
  }
  return { measurements: INVENTED_FIGURE, items: picked.map((i) => ({ id: i.id })) };
}

const GIRTH: Record<string, string> = { CHEST: 'в груди', WAIST: 'в талии', HIPS: 'в бёдрах' };

/** One fit note in plain Russian; a value the contract does not know is shown as it came, never guessed. */
export function noteText(note: FitNote): string {
  switch (note.kind) {
    case 'TOO_TIGHT':
      return `${GIRTH[note.part] ?? note.part} тесно`;
    case 'TOO_LOOSE':
      return `${GIRTH[note.part] ?? note.part} свободно`;
    case 'SLEEVE_SHORT':
      return 'рукав короче';
    case 'SLEEVE_LONG':
      return 'рукав длиннее';
    case 'LEG_SHORT':
      return 'по длине коротко';
    case 'LEG_LONG':
      return 'по длине длинно';
    default:
      return `${note.kind} ${note.part}`;
  }
}

/** One FitNote row per layer, numbered like its markers: the item's name and what the server said about the fit. */
export function fitRows(layers: Layer[], items: Item[]): Array<{ n: number; name: string; text: string; ok: boolean; drawn: boolean }> {
  const byId = new Map(items.map((i) => [i.id, i]));
  return layers.map((l, i) => {
    const name = byId.get(l.itemId)?.name ?? (TYPE_LABEL[l.type as ItemType] ?? l.itemId);
    const ok = l.fit.length === 0;
    const drawn = drawnAs(l.type) !== null;
    const text = ok ? 'замечаний нет (размер не указан)' : l.fit.map(noteText).join(', ');
    return { n: i + 1, name, text: drawn ? text : `${text}; на кукле не нарисовано`, ok, drawn };
  });
}
