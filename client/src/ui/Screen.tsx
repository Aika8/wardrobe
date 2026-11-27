// Screen: the frame of a screen. SafeAreaView keeps the safe area only; the inner View carries the padding, because
// padding on a SafeAreaView is lost in the web build (tech debt 2026-11-26-WR-035-1). Draft — вид проверяет Aika.
import type { ReactNode } from 'react';
import { SafeAreaView, StyleSheet, View } from 'react-native';
import { tokens } from './tokens.ts';

export function Screen({ children }: { children: ReactNode }) {
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.inner}>{children}</View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: tokens.colour.paper },
  inner: { flex: 1, padding: tokens.space.lg, backgroundColor: tokens.colour.paper },
});
