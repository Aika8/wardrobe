// WhyItFits (DESIGN.md): list of rules with ✓ / ✗ and a one-line reason each. Draft — вид проверяет Aika.
// The server decides each rule; this component only shows its answer.
import { StyleSheet, Text, View } from 'react-native';
import { fitMark } from './status.ts';
import { tokens } from './tokens.ts';

export type RuleLine = { rule: string; fits: boolean; reason: string };

export function WhyItFits({ lines }: { lines: readonly RuleLine[] }) {
  return (
    <View>
      {lines.map((line, i) => {
        const { mark, colour } = fitMark(line.fits);
        return (
          <View key={line.rule} style={[styles.line, i > 0 && styles.ruled]}>
            <Text style={[styles.mark, { color: colour }]} accessibilityLabel={line.fits ? 'да' : 'нет'}>{mark}</Text>
            <Text style={styles.text}>
              <Text style={styles.rule}>{line.rule}</Text> — {line.reason}
            </Text>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  line: { flexDirection: 'row', gap: tokens.space.sm, paddingVertical: tokens.space.xs },
  ruled: { borderTopWidth: tokens.line.hair, borderStyle: 'dashed', borderTopColor: tokens.colour.rule },
  mark: { width: tokens.space.lg, fontSize: tokens.font.size.sm, fontWeight: '800' },
  text: { flex: 1, fontSize: tokens.font.size.sm, color: tokens.colour.ink },
  rule: { fontWeight: '700' },
});
