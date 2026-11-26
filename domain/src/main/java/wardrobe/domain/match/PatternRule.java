package wardrobe.domain.match;

import wardrobe.domain.Item;
import wardrobe.domain.Pattern;

/**
 * Two items go together by pattern when at least one of them is {@link Pattern#PLAIN}: two patterns clash. An unknown
 * pattern is never vouched for, so it counts as a pattern (intent 01, invariant 5), except that an item always goes
 * with itself (invariant 3).
 */
public final class PatternRule implements CombinationRule {

    @Override
    public Verdict judge(Item a, Item b) {
        if (a.id().equals(b.id())) {
            return Verdict.yes("an item goes with itself");
        }
        if (a.pattern() == Pattern.PLAIN || b.pattern() == Pattern.PLAIN) {
            return Verdict.yes("a plain item goes with any pattern");
        }
        return Verdict.no("two patterns: " + describe(a) + " and " + describe(b));
    }

    private static String describe(Item item) {
        if (item.pattern() == Pattern.UNKNOWN) {
            return "unknown pattern of item " + item.id();
        }
        return item.pattern().name();
    }
}
