// Calls to our own server (ADR-0002: no React here, tested with `node --test` and a fake `fetch`).
// The server decides which status changes are allowed; this file only asks and reads the answer.
import { type Item, type ItemStatus, parseItem } from './items.ts';

type FetchFn = (url: string, init?: RequestInit) => Promise<Response>;

/** The server's `Error.message` when the body is the contract's `Error`, else a short HTTP line. */
async function errorMessage(r: Response): Promise<string> {
  try {
    const body = (await r.json()) as Record<string, unknown> | null;
    if (typeof body?.message === 'string' && body.message.length > 0) return body.message;
  } catch {
    // not JSON: fall through to the status line
  }
  return `HTTP ${r.status}`;
}

/**
 * `PUT /items/{id}/status`: moves one item to `status`. Returns the item as the server now has it, or throws an
 * `Error` whose message is the server's `Error.message` as is (a refused change leaves the item as it was).
 */
export async function changeStatus(
  baseUrl: string,
  id: string,
  status: ItemStatus,
  fetchFn: FetchFn = fetch,
): Promise<Item> {
  const r = await fetchFn(`${baseUrl}/items/${encodeURIComponent(id)}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });
  if (!r.ok) throw new Error(await errorMessage(r));
  return parseItem(await r.json());
}
