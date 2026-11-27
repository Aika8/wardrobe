// The item list screen and, on a tap, the details of one item (draft — вид проверяет Aika).
// Logic lives in src/items.ts, src/filter.ts and src/details.ts; no navigation library, just state here.
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, View } from 'react-native';
import { changeStatus } from './src/api.ts';
import { API_URL } from './src/config.ts';
import { filterByStatus, filterChips, type StatusFilter } from './src/filter.ts';
import { type Item, type ItemStatus, parseItems, sortForList, STATUS_LABEL } from './src/items.ts';
import { ItemDetails } from './src/screens/ItemDetails.tsx';
import { Chip } from './src/ui/Chip.tsx';
import { pressProps } from './src/ui/press.ts';
import { Screen } from './src/ui/Screen.tsx';
import { tokens } from './src/ui/tokens.ts';

export default function App() {
  const [items, setItems] = useState<Item[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<StatusFilter>('ALL');
  const [openId, setOpenId] = useState<string | null>(null);
  const [moving, setMoving] = useState(false);
  const [moveError, setMoveError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_URL}/items`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((body) => setItems(sortForList(parseItems(body))))
      .catch((e: unknown) => setError(e instanceof Error ? e.message : String(e)));
  }, []);

  const show = (id: string | null) => {
    setOpenId(id);
    setMoveError(null);
  };

  const move = (id: string, status: ItemStatus) => {
    setMoving(true);
    setMoveError(null);
    changeStatus(API_URL, id, status)
      .then((updated) => setItems((all) => all && sortForList(all.map((i) => (i.id === updated.id ? updated : i)))))
      .catch((e: unknown) => setMoveError(e instanceof Error ? e.message : String(e)))
      .finally(() => setMoving(false));
  };

  const open = items?.find((i) => i.id === openId);
  if (open) {
    return (
      <Screen>
        <ItemDetails
          item={open}
          onBack={() => show(null)}
          onMove={(status) => move(open.id, status)}
          busy={moving}
          error={moveError}
        />
        <StatusBar style="auto" />
      </Screen>
    );
  }

  return (
    <Screen>
      <Text style={styles.title}>Гардероб</Text>
      {error && <Text style={styles.error}>Не удалось загрузить вещи: {error}</Text>}
      {!error && !items && <ActivityIndicator />}
      {items && (
        <View style={styles.chips}>
          {filterChips(items).map((c) => (
            <Chip
              key={c.key}
              label={c.label}
              count={c.count}
              selected={c.key === filter}
              onPress={() => setFilter(c.key)}
            />
          ))}
        </View>
      )}
      {items && (
        <FlatList
          data={filterByStatus(items, filter)}
          keyExtractor={(i) => i.id}
          renderItem={({ item }) => (
            <View style={styles.row} {...pressProps(() => show(item.id), item.name)}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={item.status === 'CLEAN' ? styles.clean : styles.away}>{STATUS_LABEL[item.status]}</Text>
            </View>
          )}
        />
      )}
      <StatusBar style="auto" />
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 24, fontWeight: '600', marginBottom: 12 },
  error: { color: '#b00020' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: tokens.space.sm, marginBottom: tokens.space.md },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10 },
  name: { fontSize: 16 },
  clean: { fontSize: 14, color: '#2e7d32' },
  away: { fontSize: 14, color: '#757575' },
});
