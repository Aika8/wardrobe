// Button (DESIGN.md): primary (`terracotta` fill) and quiet (ink outline). Draft — вид проверяет Aika.
import { StyleSheet, Text, View } from 'react-native';
import { pressProps } from './press.ts';
import { tokens } from './tokens.ts';

export function Button({ label, onPress, kind = 'primary' }: {
  label: string;
  onPress: () => void;
  kind?: 'primary' | 'quiet';
}) {
  const quiet = kind === 'quiet';
  return (
    <View style={[styles.button, quiet ? styles.quiet : styles.primary]} {...pressProps(onPress, label)}>
      <Text style={[styles.label, { color: quiet ? tokens.colour.ink : tokens.colour['on-accent'] }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  button: {
    alignSelf: 'flex-start',
    borderRadius: tokens.radius.pill,
    paddingVertical: tokens.space.sm,
    paddingHorizontal: tokens.space.lg,
  },
  primary: { backgroundColor: tokens.colour.terracotta },
  quiet: { backgroundColor: 'transparent', boxShadow: `inset 0 0 0 1.5px ${tokens.colour.ink}` },
  label: { fontSize: tokens.font.size.md, fontWeight: '700' },
});
