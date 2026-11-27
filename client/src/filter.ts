// Pure logic of the status filter on the item list (ADR-0002: no React here, tested with `node --test`).
// Filtering is local: the list is small. The server's `GET /items?status=` is for later (WR-023).
import { type Item, type ItemStatus, STATUS_LABEL } from './items.ts';

export type StatusFilter = ItemStatus | 'ALL';

/** Every status in the order the list shows them (same order as `sortForList`). */
export const FILTER_STATUSES: readonly ItemStatus[] = ['CLEAN', 'IN_WASH', 'IN_REPAIR', 'ON_TRIP'];

/** The items with that status, in their order; 'ALL' gives every item. The input is not changed. */
export function filterByStatus(items: readonly Item[], status: StatusFilter): Item[] {
  return status === 'ALL' ? [...items] : items.filter((i) => i.status === status);
}

/** How many items have each status; every status is present, zero included. */
export function countsByStatus(items: readonly Item[]): Record<ItemStatus, number> {
  const counts: Record<ItemStatus, number> = { CLEAN: 0, IN_WASH: 0, IN_REPAIR: 0, ON_TRIP: 0 };
  for (const i of items) counts[i.status] += 1;
  return counts;
}

export type FilterChip = { key: StatusFilter; label: string; count: number };

const capitalised = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);

/** The chips above the list: «Все» with the total, then each status with its count, zero included. */
export function filterChips(items: readonly Item[]): FilterChip[] {
  const counts = countsByStatus(items);
  return [
    { key: 'ALL', label: 'Все', count: items.length },
    ...FILTER_STATUSES.map((s) => ({ key: s, label: capitalised(STATUS_LABEL[s]), count: counts[s] })),
  ];
}
