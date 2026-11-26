package wardrobe.domain.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import wardrobe.domain.Item;

/**
 * Pattern: Composite — several rules answer as one rule, so a caller asks "does this pair go?" once and gets every
 * reason. See docs/adr/0002-stack-and-hexagon.md.
 *
 * <p>A pair goes only when every rule says yes; then the reason names the rules in order ("goes: formality, colour,
 * pattern, season"). Otherwise the reason is the failing rules' reasons joined with "; ", in rule order. Each part is
 * symmetric and lets an item go with itself, so the whole is too (intent 01, invariant 3).
 */
public final class Combination implements CombinationRule {

    private final List<CombinationRule> rules;

    public Combination(List<CombinationRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("a combination needs at least one rule");
        }
        this.rules = List.copyOf(rules);
    }

    /** Formality, colour, pattern, season — in that order. */
    public static Combination standard() {
        return new Combination(List.of(new FormalityRule(), new ColourRule(), new PatternRule(), new SeasonRule()));
    }

    @Override
    public Verdict judge(Item a, Item b) {
        List<String> failures = new ArrayList<>();
        for (CombinationRule rule : rules) {
            Verdict v = rule.judge(a, b);
            if (!v.goes()) {
                failures.add(v.reason());
            }
        }
        if (failures.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < rules.size(); i++) {
                names.add(name(rules.get(i), i));
            }
            return Verdict.yes("goes: " + String.join(", ", names));
        }
        return Verdict.no(String.join("; ", failures));
    }

    /** "FormalityRule" → "formality"; a rule without such a class name (a lambda) → "rule N", counted from 1. */
    static String name(CombinationRule rule, int index) {
        String simple = rule.getClass().getSimpleName();
        if (simple.endsWith("Rule") && simple.length() > "Rule".length() && !rule.getClass().isSynthetic()) {
            return simple.substring(0, simple.length() - "Rule".length()).toLowerCase(Locale.ROOT);
        }
        if (rule instanceof Combination) {
            return "combination";
        }
        return "rule " + (index + 1);
    }
}
