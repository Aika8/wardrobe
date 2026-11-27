package wardrobe.domain.outfit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.match.ColourRule;
import wardrobe.domain.match.Combination;
import wardrobe.domain.match.CombinationRule;
import wardrobe.domain.match.FormalityRule;
import wardrobe.domain.match.PatternRule;
import wardrobe.domain.match.ReasonCode;
import wardrobe.domain.match.SeasonRule;
import wardrobe.domain.match.Verdict;

/**
 * Why an outfit goes or not, rule by rule: one {@link WhyLine} per rule of {@link Combination#standard()}, in its
 * order, then one for cleanliness. Nothing is decided here: the rules' verdicts and {@link OutfitCheck}'s
 * {@link OutfitProblem.NotClean} problems are only arranged into lines.
 *
 * <p>A rule's line is ✓ when every pair (in input order) passes it, else ✗ naming the first failing pair. A ✓ line is
 * about every item; its code is the first pair's code and its reason the pairs' distinct reasons joined with "; ".
 * Fewer than two items give ✓ {@link ReasonCode#NO_PAIRS} (DECISIONS.md, WR-029).
 */
public final class Explain {

    private Explain() {
    }

    public static List<WhyLine> outfit(Outfit outfit) {
        Objects.requireNonNull(outfit, "outfit");
        Combination standard = Combination.standard();
        List<WhyLine> lines = new ArrayList<>();
        for (CombinationRule rule : standard.rules()) {
            lines.add(ruleLine(ruleOf(rule), rule, outfit.items()));
        }
        lines.add(cleanLine(outfit, standard));
        return List.copyOf(lines);
    }

    private static WhyLine ruleLine(WhyLine.Rule name, CombinationRule rule, List<Item> items) {
        List<ItemId> all = ids(items);
        if (items.size() < 2) {
            return new WhyLine(name, true, ReasonCode.NO_PAIRS, all, "fewer than two items: nothing to compare");
        }
        ReasonCode firstCode = null;
        Set<String> reasons = new LinkedHashSet<>();
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                Item a = items.get(i);
                Item b = items.get(j);
                Verdict v = rule.judge(a, b);
                if (!v.goes()) {
                    return new WhyLine(name, false, v.code(), List.of(a.id(), b.id()), v.reason());
                }
                if (firstCode == null) {
                    firstCode = v.code();
                }
                reasons.add(v.reason());
            }
        }
        return new WhyLine(name, true, firstCode, all, String.join("; ", reasons));
    }

    private static WhyLine cleanLine(Outfit outfit, CombinationRule rule) {
        List<ItemId> notClean = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        for (OutfitProblem problem : OutfitCheck.check(outfit, rule)) {
            if (problem instanceof OutfitProblem.NotClean nc) {
                notClean.add(nc.item().id());
                messages.add(nc.message());
            }
        }
        if (notClean.isEmpty()) {
            return new WhyLine(WhyLine.Rule.CLEAN, true, ReasonCode.ALL_CLEAN, ids(outfit.items()),
                    "every item is clean");
        }
        return new WhyLine(WhyLine.Rule.CLEAN, false, ReasonCode.NOT_CLEAN, notClean, String.join("; ", messages));
    }

    private static WhyLine.Rule ruleOf(CombinationRule rule) {
        return switch (rule) {
            case FormalityRule r -> WhyLine.Rule.FORMALITY;
            case ColourRule r -> WhyLine.Rule.COLOUR;
            case PatternRule r -> WhyLine.Rule.PATTERN;
            case SeasonRule r -> WhyLine.Rule.SEASON;
            default -> throw new IllegalStateException("no line for rule " + rule.getClass().getName());
        };
    }

    private static List<ItemId> ids(List<Item> items) {
        return items.stream().map(Item::id).toList();
    }
}
