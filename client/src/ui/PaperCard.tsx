// PaperCard (DESIGN.md): card on paper — `card` colour, radius `card`, shadow, optional Tape. Draft — вид проверяет Aika.
import type { ReactNode } from 'react';
import { type StyleProp, StyleSheet, View, type ViewStyle } from 'react-native';
import { Tape, type TapeSide } from './Tape.tsx';
import { tokens } from './tokens.ts';

export function PaperCard({ children, tape, style }: { children?: ReactNode; tape?: TapeSide; style?: StyleProp<ViewStyle> }) {
  return (
    <View style={[styles.card, style]}>
      {tape && <Tape side={tape} />}
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    position: 'relative',
    backgroundColor: tokens.colour.card,
    borderRadius: tokens.radius.card,
    boxShadow: tokens.shadow.card,
    padding: tokens.space.md,
  },
});
