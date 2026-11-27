// PaperDoll (DESIGN.md, ADR-0003): the faceless doll of docs/design/mockups/garments.py, bare or in a shirt and
// trousers of item colours. Draft — вид проверяет Aika. Placing garments by the server's landmarks (POST /avatar/dress)
// is the avatar screen's task; this component draws the doll the script exports, nothing more.
import { useMemo } from 'react';
import { SvgXml } from 'react-native-svg';
import { DOLL_BOX, dollXml, itemFill } from './garments.ts';

/** `clothes` are the items' `Colour`s from the API; without them the doll is bare. `height` in points. */
export function PaperDoll({ clothes, height }: { clothes?: { shirt: string; trousers: string }; height: number }) {
  const shirt = clothes?.shirt;
  const trousers = clothes?.trousers;
  const xml = useMemo(
    () => dollXml(shirt !== undefined && trousers !== undefined ? { shirt: itemFill(shirt), trousers: itemFill(trousers) } : undefined),
    [shirt, trousers],
  );
  const width = (height * DOLL_BOX.width) / DOLL_BOX.height;
  return <SvgXml xml={xml} width={width} height={height} accessibilityLabel="Фигура" />;
}
