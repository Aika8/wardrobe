// FitNote (DESIGN.md, mockups.html `.fit`): a numbered round marker and one line about how an item sits.
// Draft — вид проверяет Aika. Mustard marker when the server has a note, sage when it has none.
import { StyleSheet, Text, View } from 'react-native';
import { tokens } from '../ui/tokens.ts';

export function FitNote({ n, name, text, ok }: { n: number; name: string; text: string; ok: boolean }) {
  return (
    <View style={styles.row}>
      <View style={[styles.marker, { backgroundColor: ok ? tokens.colour.sage : tokens.colour.mustard }]}>
        <Text style={styles.n}>{n}</Text>
      </View>
      <Text style={styles.text}>
        <Text style={styles.name}>{name}</Text>: {text}.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: tokens.space.sm, alignItems: 'flex-start', paddingVertical: tokens.space.xs },
  marker: { width: 18, height: 18, borderRadius: 9, alignItems: 'center', justifyContent: 'center' },
  n: { color: tokens.colour['on-accent'], fontWeight: '800', fontSize: tokens.font.size.xs },
  text: { flex: 1, fontSize: tokens.font.size.sm, color: tokens.colour.ink },
  name: { fontWeight: '700' },
});
