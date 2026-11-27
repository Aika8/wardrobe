// MeasureRow (DESIGN.md, mockups.html `.measure`): a measurement's name and its value in centimetres.
// Draft — вид проверяет Aika. A dashed rule above every row but the first.
import { StyleSheet, Text, View } from 'react-native';
import { tokens } from '../ui/tokens.ts';

export function MeasureRow({ label, value, first }: { label: string; value: string; first?: boolean }) {
  return (
    <View style={[styles.row, first ? null : styles.rule]}>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: tokens.space.xs },
  rule: { borderTopWidth: 1, borderStyle: 'dashed', borderTopColor: tokens.colour.rule },
  label: { fontSize: tokens.font.size.sm, color: tokens.colour.ink },
  value: { fontSize: tokens.font.size.sm, color: tokens.colour.ink, fontWeight: '800' },
});
