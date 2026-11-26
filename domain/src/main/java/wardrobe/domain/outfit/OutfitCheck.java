package wardrobe.domain.outfit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import wardrobe.domain.Item;
import wardrobe.domain.ItemType;
import wardrobe.domain.match.CombinationRule;
import wardrobe.domain.match.Verdict;

/**
 * Whether an outfit can be worn: every item clean, at most one item per body slot (ACCESSORY any number), a dress
 * without a top or a bottom, every pair going together by the given rule. An outfit with no BOTTOM and no DRESS is
 * not a problem in version 1 (DECISIONS.md, WR-006).
 *
 * <p>Problems come in this order: not clean, two of a kind, dress with separates, clash, empty; inside each kind,
 * in input order.
 */
public final class OutfitCheck {

    private OutfitCheck() {
    }

    /** Every problem of the outfit; an empty list means it is fine. */
    public static List<OutfitProblem> check(Outfit outfit, CombinationRule rule) {
        Objects.requireNonNull(outfit, "outfit");
        Objects.requireNonNull(rule, "rule");
        List<Item> items = outfit.items();
        List<OutfitProblem> problems = new ArrayList<>();

        for (Item item : items) {
            if (!item.availableForOutfit()) {
                problems.add(new OutfitProblem.NotClean(item));
            }
        }

        Map<ItemType, Item> firstOfType = new EnumMap<>(ItemType.class);
        for (Item item : items) {
            if (item.type() == ItemType.ACCESSORY) {
                continue;
            }
            Item first = firstOfType.putIfAbsent(item.type(), item);
            if (first != null) {
                problems.add(new OutfitProblem.TwoOfAKind(item.type(), first, item));
            }
        }

        for (Item dress : items) {
            if (dress.type() != ItemType.DRESS) {
                continue;
            }
            for (Item other : items) {
                if (other.type() == ItemType.TOP || other.type() == ItemType.BOTTOM) {
                    problems.add(new OutfitProblem.DressWithSeparates(dress, other));
                }
            }
        }

        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                Verdict v = rule.judge(items.get(i), items.get(j));
                if (!v.goes()) {
                    problems.add(new OutfitProblem.Clash(items.get(i), items.get(j), v.reason()));
                }
            }
        }

        if (items.isEmpty()) {
            problems.add(new OutfitProblem.Empty());
        }
        return List.copyOf(problems);
    }
}
