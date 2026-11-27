// The item list screen (route `list`, param = status filter, e.g. '#/list/IN_WASH'). Draft — вид проверяет Aika.
// Moved from App.tsx (WR-126) without changing its look or logic; logic lives in src/items.ts and src/filter.ts.
import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, View } from 'react-native';
import { FILTER_STATUSES, filterByStatus, filterChips, type StatusFilter } from '../filter.ts';
import { type Item, STATUS_LABEL } from '../items.ts';
import type { ScreenProps } from '../nav.ts';
import { Chip } from '../ui/Chip.tsx';
import { pressProps } from '../ui/press.ts';
import { tokens } from '../ui/tokens.ts';
import { loadItems, messageOf } from './loadItems.ts';

/** The route's param as a filter: a known status, else «Все». */
function filterOf(param: string | undefined): StatusFilter {
  return FILTER_STATUSES.find((s) => s === param) ?? 'ALL';
}

export function ItemList({ route, go, reset }: ScreenProps) {
  const [items, setItems] = useState<Item[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const filter = filterOf(route.param);

  useEffect(() => {
    loadItems()
      .then(setItems)
      .catch((e: unknown) => setError(messageOf(e)));
  }, []);

  const setFilter = (key: StatusFilter) => reset(key === 'ALL' ? { name: 'list' } : { name: 'list', param: key });

  return (
    <View style={styles.root} testID={items ? 'screen' : undefined}>
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
            <View style={styles.row} {...pressProps(() => go({ name: 'item', param: item.id }), item.name)}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={item.status === 'CLEAN' ? styles.clean : styles.away}>{STATUS_LABEL[item.status]}</Text>
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  title: { fontSize: 24, fontWeight: '600', marginBottom: 12 },
  error: { color: '#b00020' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: tokens.space.sm, marginBottom: tokens.space.md },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10 },
  name: { fontSize: 16 },
  clean: { fontSize: 14, color: '#2e7d32' },
  away: { fontSize: 14, color: '#757575' },
});
