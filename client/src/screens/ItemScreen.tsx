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

  useEffect(() => {
    setItem(undefined);
    setError(null);
    setMoveError(null);
    loadItems()
      .then((all) => setItem(all.find((i) => i.id === route.param) ?? null))
      .catch((e: unknown) => setError(messageOf(e)));
  }, [route.param]);

  const move = (id: string, status: ItemStatus) => {
    setMoving(true);
    setMoveError(null);
    changeStatus(API_URL, id, status)
      .then(setItem)
      .catch((e: unknown) => setMoveError(messageOf(e)))
      .finally(() => setMoving(false));
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
