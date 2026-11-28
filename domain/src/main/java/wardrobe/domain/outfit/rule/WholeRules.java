package wardrobe.domain.outfit.rule;

import java.util.List;
import java.util.Objects;
import wardrobe.domain.Item;

/** The standard whole-outfit rules, in a fixed order: accents, complete, accessories. */
public final class WholeRules {
    private static final List<WholeRule> STANDARD =
            List.of(new AccentRule(), new CompleteRule(), new AccessoryRule());

    private WholeRules() {
    }

    /** The three rules in the order accent, complete, accessory. */
    public static List<WholeRule> standard() {
        return STANDARD;
    }

    /** One verdict per standard rule, in the order of {@link #standard()}. */
    public static List<WholeVerdict> judgeAll(List<Item> items) {
        List<Item> copy = List.copyOf(Objects.requireNonNull(items, "items"));
        return STANDARD.stream().map(rule -> rule.judge(copy)).toList();
    }
}
