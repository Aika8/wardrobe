// The app: a history of routes and the screen on top of it (WR-126; no navigation library, ADR-0002).
// Screens are registered in src/screens/routes.tsx; the router's logic is src/nav.ts. On the web the route lives in
// `location.hash`, so a link like '#/item/white-shirt' opens that screen and the browser's back button works.
import { StatusBar } from 'expo-status-bar';
import { useEffect, useMemo, useState } from 'react';
import { back, current, type History, parseHash, push, reset, resolveRoute, type Route, sameRoute, toHash } from './src/nav.ts';
import { DEFAULT_ROUTE, ROUTES } from './src/screens/routes.tsx';
import { Screen } from './src/ui/Screen.tsx';

const KNOWN = Object.keys(ROUTES);
const web = typeof window !== 'undefined' && window.location ? window : null;
const known = (r: Route | null): Route => resolveRoute(r, KNOWN, DEFAULT_ROUTE);

export default function App() {
  const [history, setHistory] = useState<History>(() => [known(web ? parseHash(web.location.hash) : null)]);
  const route = current(history);

  // Every move writes the hash; the hashchange it causes finds the same route and does nothing.
  useEffect(() => {
    if (web && !sameRoute(parseHash(web.location.hash) ?? DEFAULT_ROUTE, route)) web.location.hash = toHash(route);
  }, [route]);

  // A hash changed from outside (browser back, a typed link): that route becomes the whole history.
  useEffect(() => {
    if (!web) return;
    const onHash = () => {
      const r = known(parseHash(web.location.hash));
      setHistory((h) => (sameRoute(current(h), r) ? h : reset(h, r)));
    };
    web.addEventListener('hashchange', onHash);
    return () => web.removeEventListener('hashchange', onHash);
  }, []);

  const props = useMemo(
    () => ({
      route,
      go: (r: Route) => setHistory((h) => push(h, r)),
      back: () => setHistory((h) => back(h)),
      reset: (r: Route) => setHistory((h) => reset(h, r)),
    }),
    [route],
  );
  const Shown = ROUTES[route.name] ?? ROUTES[DEFAULT_ROUTE.name]!;

  return (
    <Screen>
      <Shown key={route.name} {...props} />
      <StatusBar style="auto" />
    </Screen>
  );
}
