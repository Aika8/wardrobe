// The smallest router (WR-126): routes, the hash they live in on the web, and a history. Pure — no React or
// React Native here (ADR-0002), tested with `node --test`. No navigation library: a screen task adds one line to
// `src/screens/routes.tsx`.

/** One place in the app: a screen name and, for some screens, one parameter (an item id, a status filter). */
export type Route = { name: string; param?: string };

/** What every screen gets: where it is, and how to move. */
export type ScreenProps = {
  route: Route;
  go(r: Route): void;
  back(): void;
  reset(r: Route): void;
};

/** The four places of `BottomNav`; switching between them is a `reset`, not a `push`. */
export const MAIN_ROUTES: readonly string[] = ['home', 'wardrobe', 'calendar', 'me'];

/** A history, oldest first; never empty. */
export type History = readonly Route[];

/**
 * `'#/item/white-shirt'` → `{ name: 'item', param: 'white-shirt' }`. Name and param are URI-decoded; an empty
 * param is no param. `''`, `'#'`, `'#/'`, an empty name or a broken `%` escape → `null` (the caller shows the
 * default route).
 */
export function parseHash(hash: string): Route | null {
  const path = hash.replace(/^#/, '').replace(/^\//, '');
  if (path === '') return null;
  const slash = path.indexOf('/');
  const rawName = slash < 0 ? path : path.slice(0, slash);
  const rawParam = slash < 0 ? '' : path.slice(slash + 1);
  try {
    const name = decodeURIComponent(rawName);
    if (name === '') return null;
    return rawParam === '' ? { name } : { name, param: decodeURIComponent(rawParam) };
  } catch {
    return null;
  }
}

/** The hash of a route; `parseHash(toHash(r))` equals `r` for any non-empty name. */
export function toHash(route: Route): string {
  const name = encodeURIComponent(route.name);
  return route.param === undefined || route.param === '' ? `#/${name}` : `#/${name}/${encodeURIComponent(route.param)}`;
}

/** Two routes are the same place when their hashes are equal. */
export function sameRoute(a: Route, b: Route): boolean {
  return toHash(a) === toHash(b);
}

/** The route if a screen is registered under its name, else the fallback (an unknown route shows the default). */
export function resolveRoute(route: Route | null, known: readonly string[], fallback: Route): Route {
  return route !== null && known.includes(route.name) ? route : fallback;
}

/** A new history with `r` on top; the input is not changed. */
export function push(h: History, r: Route): History {
  return [...h, r];
}

/** A new history without its top; at the root it returns `h` unchanged (a history never empties). */
export function back(h: History): History {
  return h.length <= 1 ? h : h.slice(0, -1);
}

/** A tab switch: the history becomes `[r]`. */
export function reset(_h: History, r: Route): History {
  return [r];
}

/** The route on top of the history. */
export function current(h: History): Route {
  const top = h[h.length - 1];
  if (top === undefined) throw new Error('a history is never empty');
  return top;
}
