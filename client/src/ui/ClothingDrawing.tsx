// ClothingDrawing (ADR-0003): one garment drawing from docs/design/mockups/garments.py in an item's colour.
// Draft — вид проверяет Aika. The shapes live in garments.data.ts; this component only recolours and renders them.
import { useMemo } from 'react';
import { SvgXml } from 'react-native-svg';
import { type GarmentKind, garmentXml, itemFill } from './garments.ts';

/** `colour` is the item's `Colour` from the API (e.g. 'NAVY'); `size` is the side of the square in points. */
export function ClothingDrawing({ kind, colour, size }: { kind: GarmentKind; colour: string; size: number }) {
  const xml = useMemo(() => garmentXml(kind, itemFill(colour)), [kind, colour]);
  return <SvgXml xml={xml} width={size} height={size} />;
}
