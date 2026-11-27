// `GET /items`, sorted for the list: the one load both the list and the item screen use (moved from App.tsx).
import { API_URL } from '../config.ts';
import { type Item, parseItems, sortForList } from '../items.ts';

export function loadItems(): Promise<Item[]> {
  return fetch(`${API_URL}/items`)
    .then((r) => {
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      return r.json();
    })
    .then((body) => sortForList(parseItems(body)));
}

export const messageOf = (e: unknown): string => (e instanceof Error ? e.message : String(e));
