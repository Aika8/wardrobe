// Tape (DESIGN.md): a washi-tape strip on a card's top edge, slightly turned. Draft — вид проверяет Aika.
// Its size and turn are part of the drawing (as in the mockups), not tokens; the paper stripes of the mockup
// need a gradient and are left out (only View).
import { StyleSheet, View } from 'react-native';
import { tokens } from './tokens.ts';

export type TapeSide = 'left' | 'right';

export function Tape({ side = 'left' }: { side?: TapeSide }) {
  return <View style={[styles.tape, side === 'right' ? styles.right : styles.left]} pointerEvents="none" />;
}

const styles = StyleSheet.create({
  tape: {
    position: 'absolute',
    top: -9,
    width: 84,
    height: 20,
    backgroundColor: tokens.colour.tape,
    opacity: 0.75,
    borderRadius: tokens.radius.chip,
  },
  left: { left: '50%', marginLeft: -tokens.space.xxl, transform: [{ rotate: '-3deg' }] },
  right: { left: '72%', marginLeft: -tokens.space.xxl, transform: [{ rotate: '4deg' }] },
});
