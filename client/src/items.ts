// Pure logic of the item list (ADR-0002: no React here, tested with `node --test`).
// The server decides every wardrobe rule; this file only reads its answer and orders it for the screen.

export type ItemStatus = 'CLEAN' | 'IN_WASH' | 'IN_REPAIR' | 'ON_TRIP';

export type Item = {
  id: string;
  name: string;
  type: string;
  colour: string;
  pattern: string;
  fabric: string;
  seasons: string[];
  formality: string;
  status: ItemStatus;
  /** The statuses the server allows this item to move to now (its `next`); empty from a server that does not send it. */
  next: ItemStatus[];
};

const STATUSES: readonly ItemStatus[] = ['CLEAN', 'IN_WASH', 'IN_REPAIR', 'ON_TRIP'];

export const STATUS_LABEL: Record<ItemStatus, string> = {
  CLEAN: 'чистое',
  IN_WASH: 'в стирке',
  IN_REPAIR: 'в ремонте',
  ON_TRIP: 'в поездке',
};

/** The word on the button that moves an item to this status (WR-025). */
export const MOVE_LABEL: Record<ItemStatus, string> = {
  CLEAN: 'Чистое',
  IN_WASH: 'В стирку',
  IN_REPAIR: 'В ремонт',
  ON_TRIP: 'В поездку',
};

function isStringArray(v: unknown): v is string[] {
  return Array.isArray(v) && v.every((s) => typeof s === 'string');
}

/**
 * Reads one item of the contract (`Item` in `api/openapi.yaml`). Throws on a shape the contract does not allow,
 * instead of showing half an item. `next` may be absent (an older server): then no moves are offered.
 */
export function parseItem(raw: unknown, where = 'item'): Item {
  const o = raw as Record<string, unknown>;
  for (const k of ['id', 'name', 'type', 'colour', 'pattern', 'fabric', 'formality', 'status']) {
    if (typeof o?.[k] !== 'string') throw new Error(`${where}: '${k}' is not a string`);
  }
  if (!isStringArray(o.seasons)) throw new Error(`${where}: 'seasons' is not a list of strings`);
  if (!STATUSES.includes(o.status as ItemStatus)) throw new Error(`${where}: unknown status '${String(o.status)}'`);
  let next: ItemStatus[] = [];
  if (o.next !== undefined) {
    if (!isStringArray(o.next)) throw new Error(`${where}: 'next' is not a list of strings`);
    const unknown = o.next.find((s) => !STATUSES.includes(s as ItemStatus));
    if (unknown !== undefined) throw new Error(`${where}: unknown status '${unknown}' in 'next'`);
    next = o.next as ItemStatus[];
  }
  return {
    id: o.id as string,
    name: o.name as string,
    type: o.type as string,
    colour: o.colour as string,
    pattern: o.pattern as string,
    fabric: o.fabric as string,
    seasons: o.seasons,
    formality: o.formality as string,
    status: o.status as ItemStatus,
    next,
  };
}

/** Reads `GET /items`. Throws on a shape the contract does not allow, instead of showing half an item. */
export function parseItems(body: unknown): Item[] {
  if (!Array.isArray(body)) throw new Error('items: expected an array');
  return body.map((raw, i) => parseItem(raw, `items[${i}]`));
}

/** Clean items first (they can be worn today), then by status order, then by name; the input is not changed. */
export function sortForList(items: readonly Item[]): Item[] {
  return [...items].sort(
    (a, b) => STATUSES.indexOf(a.status) - STATUSES.indexOf(b.status) || a.name.localeCompare(b.name),
  );
}
