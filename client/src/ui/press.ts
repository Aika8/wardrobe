// Press handling with plain View props (the UI kit uses only View, Text and StyleSheet; WR-028).
import type { ViewProps } from 'react-native';

export function pressProps(onPress: (() => void) | undefined, label: string): ViewProps {
  if (!onPress) return {};
  return {
    accessible: true,
    accessibilityRole: 'button',
    accessibilityLabel: label,
    onStartShouldSetResponder: () => true,
    onResponderRelease: () => onPress(),
  };
}
