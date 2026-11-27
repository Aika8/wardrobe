package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Item;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.Pattern;
import wardrobe.domain.RandomItems;
import wardrobe.domain.Season;

/**
 * Properties of every pair rule over all ordered pairs of 200 random invented items, for fixed seeds (intent 01,
 * invariants 3, 4 and 8). Collects the first failures and fails once with all of them.
 */
class PairRulePropertiesTest {

    static final long[] SEEDS = {1, 2, 3};
    static final int ITEMS = 200;
    static final int MAX_FAILURES = 5;

    /** The expected answer of a rule for two different items, or empty when the oracle does not decide it. */
    interface Oracle extends BiFunction<Item, Item, Optional<Boolean>> {
    }

    static final Map<String, CombinationRule> RULES = Map.of(
            "FormalityRule", new FormalityRule(),
            "ColourRule", new ColourRule(),
            "PatternRule", new PatternRule(),
            "SeasonRule", new SeasonRule());

    static final Map<String, Oracle> ORACLES = Map.of(
            "FormalityRule", (a, b) -> Optional.of(a.formality().stepsTo(b.formality()) <= 1),
            "SeasonRule", (a, b) -> {
                EnumSet<Season> shared = EnumSet.copyOf(a.seasons());
                shared.retainAll(b.seasons());
                return Optional.of(!shared.isEmpty());
            },
            "PatternRule", (a, b) -> Optional.of(
                    a.id().equals(b.id()) || a.pattern() == Pattern.PLAIN || b.pattern() == Pattern.PLAIN),
            "ColourRule", (a, b) -> {
                if (a.id().equals(b.id())) {
                    return Optional.empty();
                }
                if (a.colour() == Colour.UNKNOWN || b.colour() == Colour.UNKNOWN) {
                    return Optional.of(false);
                }
                if (a.colour().neutral() || b.colour().neutral()) {
                    return Optional.of(true);
                }
                return Optional.empty();
            });

    private final List<String> failures = new ArrayList<>();
    private int failureCount;

    @Test
    void everyPairRuleHoldsItsPropertiesOverRandomItems() {
        long pairs = 0;
        for (long seed : SEEDS) {
            List<Item> items = RandomItems.catalogue(seed, ITEMS);
            for (String name : List.of("FormalityRule", "ColourRule", "PatternRule", "SeasonRule")) {
                CombinationRule rule = RULES.get(name);
                Oracle oracle = ORACLES.get(name);
                for (Item a : items) {
                    Verdict self = rule.judge(a, a);
                    check(self.goes(), name, "self", seed, a, a);
                    check(isReasoned(self), name, "reason not blank", seed, a, a);
                    for (Item b : items) {
                        pairs++;
                        Verdict ab = rule.judge(a, b);
                        Verdict again = rule.judge(a, b);
                        Verdict ba = rule.judge(b, a);
                        check(ab.goes() == ba.goes(), name, "symmetry", seed, a, b);
                        check(ab.equals(again), name, "determinism", seed, a, b);
                        check(isReasoned(ab), name, "reason not blank", seed, a, b);
                        Optional<Boolean> expected = oracle.apply(a, b);
                        check(expected.isEmpty() || expected.get() == ab.goes(), name,
                                "oracle (expected " + expected.orElse(null) + ", got " + ab.goes() + ")", seed, a, b);
                    }
                }
            }
        }
        assertEquals((long) SEEDS.length * RULES.size() * ITEMS * ITEMS, pairs, "every ordered pair is judged");
        if (failureCount > 0) {
            fail(failureCount + " property failures; first " + failures.size() + ":\n" + String.join("\n", failures));
        }
    }

    @Test
    void theSameSeedGivesTheSameItemsAndAnotherSeedOthers() {
        assertEquals(RandomItems.catalogue(1, ITEMS), RandomItems.catalogue(1, ITEMS));
        assertNotEquals(RandomItems.catalogue(1, ITEMS), RandomItems.catalogue(2, ITEMS));
    }

    @Test
    void theCatalogueCoversUnknownFieldsAndEveryStatus() {
        List<Item> items = RandomItems.catalogue(1, ITEMS);
        assertTrue(items.stream().anyMatch(i -> i.colour() == Colour.UNKNOWN), "some colour UNKNOWN");
        assertTrue(items.stream().anyMatch(i -> i.pattern() == Pattern.UNKNOWN), "some pattern UNKNOWN");
        assertTrue(items.stream().anyMatch(i -> i.fabric() == Fabric.UNKNOWN), "some fabric UNKNOWN");
        assertEquals(EnumSet.allOf(ItemStatus.class),
                EnumSet.copyOf(items.stream().map(Item::status).toList()), "every status appears");
        assertTrue(items.stream().allMatch(i -> i.seasons().size() >= 1 && i.seasons().size() <= 4));
        assertEquals("r0", items.get(0).id().value());
        assertEquals("r" + (ITEMS - 1), items.get(ITEMS - 1).id().value());
        assertEquals("random r0", items.get(0).name());
    }

    private static boolean isReasoned(Verdict v) {
        return v.reason() != null && !v.reason().isBlank();
    }

    private void check(boolean ok, String rule, String property, long seed, Item a, Item b) {
        if (ok) {
            return;
        }
        failureCount++;
        if (failures.size() < MAX_FAILURES) {
            failures.add(rule + " / " + property + " / seed " + seed + "\n  a = " + a + "\n  b = " + b);
        }
    }
}
