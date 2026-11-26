// StatusDot (DESIGN.md): small dot in the status colour, with the word next to it where there is room.
// Draft — вид проверяет Aika. Without the word it carries the word as its accessibility label.
import { StyleSheet, Text, View } from 'react-native';
import type { ItemStatus } from '../items.ts';
import { statusColour, statusWord } from './status.ts';
import { tokens } from './tokens.ts';

export function StatusDot({ status, withWord = true }: { status: ItemStatus; withWord?: boolean }) {
  const word = statusWord(status);
  return (
    <View style={styles.row} accessible accessibilityLabel={word}>
      <View style={[styles.dot, { backgroundColor: statusColour(status) }]} />
      {withWord && <Text style={styles.word}>{word}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: tokens.space.xs },
  dot: { width: tokens.space.sm, height: tokens.space.sm, borderRadius: tokens.radius.pill },
  word: { fontSize: tokens.font.size.xs, fontWeight: '700', color: tokens.colour['ink-soft'] },
});
