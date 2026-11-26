// StatusBadge (DESIGN.md): pill — status colour at low strength + status word. Draft — вид проверяет Aika.
import { StyleSheet, Text, View } from 'react-native';
import type { ItemStatus } from '../items.ts';
import { statusColour, statusFill, statusWord } from './status.ts';
import { tokens } from './tokens.ts';

export function StatusBadge({ status }: { status: ItemStatus }) {
  return (
    <View style={[styles.badge, { backgroundColor: statusFill(status) }]}>
      <View style={[styles.dot, { backgroundColor: statusColour(status) }]} />
      <Text style={styles.word}>{statusWord(status)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: tokens.space.xs,
    borderRadius: tokens.radius.pill,
    paddingVertical: tokens.space.xxs,
    paddingHorizontal: tokens.space.sm,
  },
  dot: { width: tokens.space.sm, height: tokens.space.sm, borderRadius: tokens.radius.pill },
  word: { fontSize: tokens.font.size.xs, fontWeight: '700', color: tokens.colour.ink },
});
