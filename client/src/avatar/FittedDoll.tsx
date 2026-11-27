// PaperDoll (DESIGN.md) fitted to the server's figure: the template doll with its points moved onto the figure's
// lines, the drawn layers in the answer's order and the fit markers. Draft — вид проверяет Aika. Logic: fit.ts.
import { useMemo } from 'react';
import { SvgXml } from 'react-native-svg';
import { avatarXml, bands, type Colours } from './fit.ts';
import type { DressedAvatar } from './types.ts';

export function FittedDoll({ answer, colours, height }: { answer: DressedAvatar; colours: Colours; height: number }) {
  const { xml, width } = useMemo(() => {
    const b = bands(answer, height);
    return { xml: avatarXml(answer, colours, b), width: b.width };
  }, [answer, colours, height]);
  return <SvgXml xml={xml} width={width} height={height} accessibilityLabel="Фигура" />;
}
