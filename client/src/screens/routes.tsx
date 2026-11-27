// Every screen of the app under its route name (WR-126). A screen task adds one line here.
import type React from 'react';
import { type Route, type ScreenProps } from '../nav.ts';
import { Avatar } from './Avatar.tsx';
import { ItemList } from './ItemList.tsx';
import { ItemScreen } from './ItemScreen.tsx';
import { Sheet } from './Sheet.tsx';

export const ROUTES: Record<string, (p: ScreenProps) => React.JSX.Element> = {
  list: ItemList,
  item: ItemScreen,
  sheet: Sheet,
  me: Avatar,
};

/** Shown at start and for any unknown route. The home task changes it. */
export const DEFAULT_ROUTE: Route = { name: 'list' };
