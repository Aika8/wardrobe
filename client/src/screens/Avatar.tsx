// Route `me` (param = item ids, comma-separated; none: the first top and bottom): the doll dressed by the server
// (POST /avatar/dress) on the one invented figure (WR-027). Draft — вид проверяет Aika. Layout of the mockup
// «6 · Кукла для примерки»: the doll and the measurements on one card, «Как сидит» with the fit notes below.
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { dress } from '../avatar/api.ts';
import { FitNote } from '../avatar/FitNote.tsx';
import { FittedDoll } from '../avatar/FittedDoll.tsx';
import { MeasureRow } from '../avatar/MeasureRow.tsx';
import { fitRows, INVENTED_FIGURE, measureRows, outfitRequest } from '../avatar/outfit.ts';
import type { DressedAvatar } from '../avatar/types.ts';
import { API_URL } from '../config.ts';
import type { Item } from '../items.ts';
import type { ScreenProps } from '../nav.ts';
import { Button } from '../ui/Button.tsx';
import { PaperCard } from '../ui/PaperCard.tsx';
import { ScreenTitle } from '../ui/ScreenTitle.tsx';
import { SectionTitle } from '../ui/SectionTitle.tsx';
import { tokens } from '../ui/tokens.ts';
import { loadItems, messageOf } from './loadItems.ts';

const DOLL_HEIGHT = 360;

export function Avatar({ route, back }: ScreenProps) {
  const [state, setState] = useState<{ items: Item[]; answer: DressedAvatar } | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setState(null);
    setError(null);
    loadItems()
      .then(async (items) => ({ items, answer: await dress(API_URL, outfitRequest(items, route.param)) }))
      .then(setState)
      .catch((e: unknown) => setError(messageOf(e)));
  }, [route.param]);

  const colours = useMemo(
    () => Object.fromEntries((state?.items ?? []).map((i) => [i.id, i.colour])),
    [state],
  );

  return (
    <ScrollView style={styles.root} testID={state ? 'screen' : undefined}>
      <View style={styles.top}>
        <Button label="‹ Назад" kind="quiet" onPress={back} />
        <Text style={styles.draft}>черновик · вид проверяет Aika</Text>
      </View>
      <ScreenTitle title="Примерка" subtitle="Фигура по меркам, вещи — слоями" />
      {error !== null && <Text style={styles.error}>Не удалось одеть фигуру: {error}</Text>}
      {error === null && state === null && <ActivityIndicator />}
      {state && (
        <>
          <PaperCard style={styles.dollCard}>
            <FittedDoll answer={state.answer} colours={colours} height={DOLL_HEIGHT} />
            <View style={styles.measures}>
              <Text style={styles.measureTitle}>Мерки</Text>
              {measureRows(INVENTED_FIGURE).map((r, i) => (
                <MeasureRow key={r.label} label={r.label} value={r.value} first={i === 0} />
              ))}
              <Text style={styles.note}>Придуманная фигура.</Text>
            </View>
          </PaperCard>
          <SectionTitle title="Как сидит" />
          <PaperCard style={styles.fitCard}>
            {state.answer.layers.length === 0 && <Text style={styles.note}>Вещей нет — фигура без одежды.</Text>}
            {fitRows(state.answer.layers, state.items).map((r) => (
              <FitNote key={r.n} n={r.n} name={r.name} text={r.text} ok={r.ok} />
            ))}
          </PaperCard>
        </>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  top: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  draft: { fontSize: tokens.font.size.xs, color: tokens.colour['ink-faint'] },
  error: { marginTop: tokens.space.md, fontSize: tokens.font.size.sm, color: tokens.colour.terracotta },
  dollCard: { flexDirection: 'row', gap: tokens.space.sm, padding: tokens.space.sm },
  measures: { flex: 1, minWidth: 0 },
  measureTitle: { fontSize: tokens.font.size.section, fontWeight: '700', color: tokens.colour.ink },
  note: { fontSize: tokens.font.size.xs, color: tokens.colour['ink-faint'], marginTop: tokens.space.xs },
  fitCard: { paddingVertical: tokens.space.sm, paddingHorizontal: tokens.space.md },
});
