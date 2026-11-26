// SectionTitle (DESIGN.md): handwritten section title with an optional link on the right. Draft — вид проверяет Aika.
// Handwriting needs expo-font (waits for an ADR); until then the system font, bold.
import { StyleSheet, Text, View } from 'react-native';
import { tokens } from './tokens.ts';

export function SectionTitle({ title, link }: { title: string; link?: { label: string; onPress: () => void } }) {
  return (
    <View style={styles.row}>
      <Text style={styles.title} accessibilityRole="header">{title}</Text>
      {link && (
        <Text style={styles.link} accessibilityRole="link" onPress={link.onPress}>
          {link.label}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'baseline',
    marginTop: tokens.space.lg,
    marginBottom: tokens.space.sm,
    marginHorizontal: tokens.space.xxs,
  },
  title: { fontSize: tokens.font.size.section, fontWeight: '700', color: tokens.colour.ink },
  link: { fontSize: tokens.font.size.sm, fontWeight: '700', color: tokens.colour.terracotta },
});
