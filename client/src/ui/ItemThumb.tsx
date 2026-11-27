// ItemThumb (DESIGN.md): an item drawing on a small card (round or square), optional StatusDot.
// Draft — вид проверяет Aika. An item would show its cut-out photo; version 1 has no photos (the API has no photo
// field), so it always shows the ClothingDrawing of its type — never a box. A type the contract does not know gets
// no drawing (nothing is guessed): the card stays empty and the item's name is its accessibility label.
import { StyleSheet, View } from 'react-native';
import type { Item } from '../items.ts';
import { ClothingDrawing } from './ClothingDrawing.tsx';
import { garmentKind } from './garments.ts';
import { StatusDot } from './StatusDot.tsx';
import { tokens } from './tokens.ts';

/** Share of the card the drawing takes, as in the mockups (`.thumb svg { width: 66% }`). */
const DRAWING_SHARE = 0.66;

export function ItemThumb({
  item,
  shape = 'round',
  size = 62,
  withStatus = false,
}: {
  item: Pick<Item, 'name' | 'type' | 'colour' | 'status'>;
  shape?: 'round' | 'square';
  /** Side of the card in points (a drawing size, not a token: DESIGN.md). */
  size?: number;
  withStatus?: boolean;
}) {
  const kind = garmentKind(item.type);
  return (
    <View
      style={[
        styles.card,
        { width: size, height: size, borderRadius: shape === 'round' ? tokens.radius.pill : tokens.radius.chip },
      ]}
      accessible
      accessibilityLabel={item.name}
    >
      {kind && <ClothingDrawing kind={kind} colour={item.colour} size={Math.round(size * DRAWING_SHARE)} />}
      {withStatus && (
        <View style={styles.status}>
          <StatusDot status={item.status} withWord={false} />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    position: 'relative',
    backgroundColor: tokens.colour.card,
    boxShadow: tokens.shadow.card,
    alignItems: 'center',
    justifyContent: 'center',
  },
  status: { position: 'absolute', right: tokens.space.xs, bottom: tokens.space.xs },
});
