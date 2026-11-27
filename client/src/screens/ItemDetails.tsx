// The item details screen (draft — вид проверяет Aika). Rows come from src/details.ts; built from the UI kit only.
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { describeItem } from '../details.ts';
import type { Item } from '../items.ts';
import { Button } from '../ui/Button.tsx';
import { PaperCard } from '../ui/PaperCard.tsx';
import { ScreenTitle } from '../ui/ScreenTitle.tsx';
import { StatusBadge } from '../ui/StatusBadge.tsx';
import { tokens } from '../ui/tokens.ts';

export function ItemDetails({ item, onBack }: { item: Item; onBack: () => void }) {
  return (
    <ScrollView contentContainerStyle={styles.content}>
      <View style={styles.top}>
        <Button label="‹ Назад" kind="quiet" onPress={onBack} />
        <StatusBadge status={item.status} />
      </View>
      <ScreenTitle title={item.name} />
      <PaperCard style={styles.card}>
        {describeItem(item).map((row, i) => (
          <View key={row.label} style={[styles.row, i > 0 && styles.ruled]}>
            <Text style={styles.label}>{row.label}</Text>
            <View style={styles.valueBox}>
              <Text style={styles.value}>{row.value}</Text>
              {row.hint !== undefined && <Text style={styles.hint}>{row.hint}</Text>}
            </View>
          </View>
        ))}
      </PaperCard>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingBottom: tokens.space.xl },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: tokens.space.md },
  card: { marginTop: tokens.space.md, paddingVertical: tokens.space.xs },
  row: { flexDirection: 'row', justifyContent: 'space-between', gap: tokens.space.md, paddingVertical: tokens.space.sm },
  ruled: { borderTopWidth: tokens.line.hair, borderTopColor: tokens.colour.rule },
  label: { fontSize: tokens.font.size.sm, color: tokens.colour['ink-soft'] },
  valueBox: { flexShrink: 1, alignItems: 'flex-end' },
  value: { fontSize: tokens.font.size.md, fontWeight: '600', color: tokens.colour.ink, textAlign: 'right' },
  hint: { fontSize: tokens.font.size.xs, color: tokens.colour['ink-soft'], textAlign: 'right' },
});
