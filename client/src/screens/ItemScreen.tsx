// Route `item` (param = item id): loads the items, finds this one and shows ItemDetails (WR-024/WR-025).
// Draft — вид проверяет Aika. The server decides which moves are allowed; this screen only asks and shows the answer.
import { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { changeStatus } from '../api.ts';
import { API_URL } from '../config.ts';
import type { Item, ItemStatus } from '../items.ts';
import type { ScreenProps } from '../nav.ts';
import { Button } from '../ui/Button.tsx';
import { tokens } from '../ui/tokens.ts';
import { ItemDetails } from './ItemDetails.tsx';
import { loadItems, messageOf } from './loadItems.ts';

export function ItemScreen({ route, back }: ScreenProps) {
  // undefined: still loading; null: loaded, no item with this id.
  const [item, setItem] = useState<Item | null | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [moving, setMoving] = useState(false);
  const [moveError, setMoveError] = useState<string | null>(null);
  // The param this screen shows now, read by a move answer that arrives late (AUTO-20261126-5). A mutable holder
  // kept in state, not useRef: the screen tests' fake React has hooks by call order and no useRef, so this stays
  // the last hook (see DECISIONS.md).
  const [shown] = useState<{ param: string | undefined }>({ param: route.param });

  useEffect(() => {
    shown.param = route.param;
    setItem(undefined);
    setError(null);
    setMoving(false);
    setMoveError(null);
    // An answer for an earlier param that arrives after the param changed is dropped (AUTO-20261126-4).
    let live = true;
    loadItems()
      .then((all) => {
        if (live) setItem(all.find((i) => i.id === route.param) ?? null);
      })
      .catch((e: unknown) => {
        if (live) setError(messageOf(e));
      });
    return () => {
      live = false;
    };
  }, [route.param]);

  const move = (id: string, status: ItemStatus) => {
    // An answer for a move started on an earlier param is dropped: it must not replace the item shown now.
    const param = route.param;
    const current = () => shown.param === param;
    setMoving(true);
    setMoveError(null);
    changeStatus(API_URL, id, status)
      .then((moved) => {
        if (current()) setItem(moved);
      })
      .catch((e: unknown) => {
        if (current()) setMoveError(messageOf(e));
      })
      .finally(() => {
        if (current()) setMoving(false);
      });
  };

  if (error !== null || item === null) {
    return (
      <View style={styles.root}>
        <Button label="‹ Назад" kind="quiet" onPress={back} />
        <Text style={styles.error}>{error !== null ? `Не удалось загрузить вещи: ${error}` : 'Такой вещи нет'}</Text>
      </View>
    );
  }
  if (item === undefined) return <ActivityIndicator />;
  return (
    <View style={styles.root} testID="screen">
      <ItemDetails item={item} onBack={back} onMove={(status) => move(item.id, status)} busy={moving} error={moveError} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  error: { marginTop: tokens.space.md, fontSize: tokens.font.size.sm, color: tokens.colour.terracotta },
});
