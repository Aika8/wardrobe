// Route `sheet` ('#/sheet'): every component of the UI kit, one sample each, under its name (WR-126). A developer
// page: no link leads to it. A component task appends one entry to SHEET. Texts are invented. Draft — вид проверяет Aika.
import type React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Button } from '../ui/Button.tsx';
import { Chip } from '../ui/Chip.tsx';
import { PaperCard } from '../ui/PaperCard.tsx';
import { ScreenTitle } from '../ui/ScreenTitle.tsx';
import { SectionTitle } from '../ui/SectionTitle.tsx';
import { StatusBadge } from '../ui/StatusBadge.tsx';
import { StatusDot } from '../ui/StatusDot.tsx';
import { tokens } from '../ui/tokens.ts';
import { WhyItFits } from '../ui/WhyItFits.tsx';

const nothing = () => {};

export const SHEET: { name: string; render: () => React.JSX.Element }[] = [
  { name: 'ScreenTitle', render: () => <ScreenTitle title="Мой гардероб" subtitle="24 вещи, 3 в стирке" /> },
  { name: 'SectionTitle', render: () => <SectionTitle title="Сегодня" link={{ label: 'все', onPress: nothing }} /> },
  { name: 'PaperCard', render: () => <PaperCard><Text>Карточка на бумаге</Text></PaperCard> },
  { name: 'PaperCard + Tape', render: () => <PaperCard tape="left"><Text>С полоской скотча</Text></PaperCard> },
  {
    name: 'Chip',
    render: () => (
      <View style={styles.row}>
        <Chip label="Все" count={24} selected onPress={nothing} />
        <Chip label="В стирке" count={3} onPress={nothing} />
      </View>
    ),
  },
  { name: 'StatusBadge', render: () => <StatusBadge status="IN_WASH" /> },
  {
    name: 'StatusDot',
    render: () => (
      <View style={styles.row}>
        <StatusDot status="CLEAN" />
        <StatusDot status="IN_REPAIR" withWord={false} />
      </View>
    ),
  },
  {
    name: 'Button',
    render: () => (
      <View style={styles.row}>
        <Button label="Надеть" onPress={nothing} />
        <Button label="Позже" kind="quiet" onPress={nothing} />
      </View>
    ),
  },
  {
    name: 'WhyItFits',
    render: () => (
      <WhyItFits
        lines={[
          { rule: 'Цвет', fits: true, reason: 'светлый верх к тёмному низу' },
          { rule: 'Сезон', fits: false, reason: 'шерсть не для лета' },
        ]}
      />
    ),
  },
];

export function Sheet(_props: unknown) {
  return (
    <View style={styles.root} testID="screen">
      <ScrollView contentContainerStyle={styles.content}>
        <ScreenTitle title="Компоненты" />
        {SHEET.map((entry) => (
          <View key={entry.name} style={styles.entry}>
            <Text style={styles.name}>{entry.name}</Text>
            {entry.render()}
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  content: { paddingBottom: tokens.space.xl },
  entry: { marginTop: tokens.space.lg },
  name: { fontSize: tokens.font.size.xs, color: tokens.colour['ink-soft'], marginBottom: tokens.space.sm },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: tokens.space.sm },
});
