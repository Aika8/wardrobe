package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.TestItems.item;

import org.junit.jupiter.api.Test;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;

/** Intent 01, invariants 3 and 4. */
class FormalityRuleTest {

    final FormalityRule rule = new FormalityRule();

    @Test
    void neighboursOnTheScaleCombineAndFartherOnesDoNot() {
        assertTrue(rule.judge(item("a", Formality.CASUAL), item("b", Formality.SMART_CASUAL)).goes());
        assertTrue(rule.judge(item("a", Formality.BUSINESS), item("b", Formality.BUSINESS)).goes());
        Verdict v = rule.judge(item("a", Formality.SPORT), item("b", Formality.BUSINESS));
        assertFalse(v.goes());
        assertTrue(v.reason().contains("3 steps"), v.reason());
    }

    @Test
    void theRuleIsSymmetricAndAnItemGoesWithItself() {
        for (Formality fa : Formality.values()) {
            Item a = item("a", fa);
            assertTrue(rule.judge(a, a).goes(), fa.name());
            for (Formality fb : Formality.values()) {
                Item b = item("b", fb);
                assertEquals(rule.judge(a, b).goes(), rule.judge(b, a).goes(), fa + " / " + fb);
                assertEquals(Math.abs(fa.ordinal() - fb.ordinal()) <= 1, rule.judge(a, b).goes(), fa + " / " + fb);
            }
        }
    }
}
