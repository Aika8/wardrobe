// ScreenTitle (DESIGN.md): handwritten title, optional subtitle in body font. Draft — вид проверяет Aika.
// Handwriting needs expo-font (waits for an ADR); until then the system font, bold.
import { StyleSheet, Text, View } from 'react-native';
import { tokens } from './tokens.ts';

export function ScreenTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <View>
      <Text style={styles.title} accessibilityRole="header">{title}</Text>
      {subtitle !== undefined && <Text style={styles.subtitle}>{subtitle}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: tokens.font.size.hero,
    fontWeight: '700',
    color: tokens.colour.ink,
    marginTop: tokens.space.xs,
    marginBottom: tokens.space.xxs,
  },
  subtitle: { fontSize: tokens.font.size.sm, color: tokens.colour['ink-soft'], marginBottom: tokens.space.md },
});
