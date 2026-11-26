// Chip (DESIGN.md): filter or attribute pill; selected = ink outline. Draft — вид проверяет Aika.
import { StyleSheet, Text, View } from 'react-native';
import { pressProps } from './press.ts';
import { tokens } from './tokens.ts';

export function Chip({ label, count, selected = false, onPress }: {
  label: string;
  count?: number;
  selected?: boolean;
  onPress?: () => void;
}) {
  return (
    <View
      style={[styles.chip, selected && styles.selected]}
      accessibilityState={{ selected }}
      {...pressProps(onPress, label)}
    >
      <Text style={styles.label} numberOfLines={1}>
        {label}
        {count !== undefined && <Text style={styles.count}> {count}</Text>}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: {
    alignSelf: 'flex-start',
    borderRadius: tokens.radius.chip,
    paddingVertical: tokens.space.xs,
    paddingHorizontal: tokens.space.md,
    backgroundColor: tokens.colour.card,
    boxShadow: tokens.shadow.card,
  },
  selected: {
    backgroundColor: tokens.colour['paper-deep'],
    boxShadow: `inset 0 0 0 1.5px ${tokens.colour.ink}`,
  },
  label: { fontSize: tokens.font.size.sm, fontWeight: '600', color: tokens.colour.ink },
  count: { fontWeight: '700', color: tokens.colour['ink-soft'] },
});
