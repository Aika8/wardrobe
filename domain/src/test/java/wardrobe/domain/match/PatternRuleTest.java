package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.match.PatternItems.item;

import org.junit.jupiter.api.Test;
import wardrobe.domain.Item;
import wardrobe.domain.Pattern;

/** Intent 01, invariants 3 and 5: at most one patterned item in a pair; UNKNOWN counts as a pattern. */
class PatternRuleTest {

    final PatternRule rule = new PatternRule();

    @Test
    void plainGoesWithEveryPatternUnknownIncluded() {
        for (Pattern p : Pattern.values()) {
            assertTrue(rule.judge(item("a", Pattern.PLAIN), item("b", p)).goes(), p.name());
            assertTrue(rule.judge(item("a", p), item("b", Pattern.PLAIN)).goes(), p.name());
        }
    }

    @Test
    void twoNonPlainPatternsDoNotGoAndTheReasonNamesBoth() {
        Verdict v = rule.judge(item("a", Pattern.STRIPED), item("b", Pattern.FLORAL));
        assertFalse(v.goes());
        assertEquals("two patterns: STRIPED and FLORAL", v.reason());

        Verdict same = rule.judge(item("a", Pattern.CHECKED), item("b", Pattern.CHECKED));
        assertFalse(same.goes(), "two different checked items are still two patterns");
    }

    @Test
    void anUnknownPatternCountsAsAPatternAndTheReasonNamesTheItem() {
        Verdict v = rule.judge(item("a", Pattern.DOTTED), item("b", Pattern.UNKNOWN));
        assertFalse(v.goes());
        assertTrue(v.reason().contains("DOTTED") && v.reason().contains("unknown") && v.reason().contains("b"),
                v.reason());
        assertFalse(rule.judge(item("a", Pattern.UNKNOWN), item("b", Pattern.UNKNOWN)).goes());
    }

    @Test
    void aPairGoesExactlyWhenOneSideIsPlain() {
        for (Pattern pa : Pattern.values()) {
            for (Pattern pb : Pattern.values()) {
                boolean expected = pa == Pattern.PLAIN || pb == Pattern.PLAIN;
                assertEquals(expected, rule.judge(item("a", pa), item("b", pb)).goes(), pa + " / " + pb);
            }
        }
    }

    @Test
    void theRuleIsSymmetricOverEveryPairOfPatterns() {
        for (Pattern pa : Pattern.values()) {
            for (Pattern pb : Pattern.values()) {
                Item a = item("a", pa);
                Item b = item("b", pb);
                assertEquals(rule.judge(a, b).goes(), rule.judge(b, a).goes(), pa + " / " + pb);
            }
        }
    }

    @Test
    void everyItemGoesWithItselfWhateverItsPattern() {
        for (Pattern p : Pattern.values()) {
            Item a = item("a", p);
            assertTrue(rule.judge(a, a).goes(), p.name());
            assertTrue(rule.judge(a, item("a", p)).goes(), p.name() + " (same id, another instance)");
        }
    }
}
