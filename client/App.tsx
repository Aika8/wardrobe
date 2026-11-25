// The item list screen (draft — вид проверяет Aika). Logic lives in src/items.ts.
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { API_URL } from './src/config.ts';
import { type Item, parseItems, sortForList, STATUS_LABEL } from './src/items.ts';

export default function App() {
  const [items, setItems] = useState<Item[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API_URL}/items`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((body) => setItems(sortForList(parseItems(body))))
      .catch((e: unknown) => setError(e instanceof Error ? e.message : String(e)));
  }, []);

  return (
    <SafeAreaView style={styles.screen}>
      <Text style={styles.title}>Гардероб</Text>
      {error && <Text style={styles.error}>Не удалось загрузить вещи: {error}</Text>}
      {!error && !items && <ActivityIndicator />}
      {items && (
        <FlatList
          data={items}
          keyExtractor={(i) => i.id}
          renderItem={({ item }) => (
            <View style={styles.row}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={item.status === 'CLEAN' ? styles.clean : styles.away}>{STATUS_LABEL[item.status]}</Text>
            </View>
          )}
        />
      )}
      <StatusBar style="auto" />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, padding: 16 },
  title: { fontSize: 24, fontWeight: '600', marginBottom: 12 },
  error: { color: '#b00020' },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10 },
  name: { fontSize: 16 },
  clean: { fontSize: 14, color: '#2e7d32' },
  away: { fontSize: 14, color: '#757575' },
});
