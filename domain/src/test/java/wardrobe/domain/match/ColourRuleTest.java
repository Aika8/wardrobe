package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.match.ColourItems.item;

import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Item;

/** Intent 01, invariants 3 and 5; the friends table is in DECISIONS.md. */
class ColourRuleTest {

    final ColourRule rule = new ColourRule();

    @Test
    void anUnknownColourDoesNotGoAndTheReasonNamesTheItem() {
        Verdict v = rule.judge(item("a", Colour.BLACK), item("b", Colour.UNKNOWN));
        assertFalse(v.goes());
        assertTrue(v.reason().contains("b") && v.reason().contains("unknown"), v.reason());
        assertFalse(v.reason().contains("item a"), v.reason());

        Verdict both = rule.judge(item("a", Colour.UNKNOWN), item("b", Colour.UNKNOWN));
        assertFalse(both.goes());
        assertTrue(both.reason().contains("a") && both.reason().contains("b"), both.reason());
    }

    @Test
    void aNeutralGoesWithAnyKnownColour() {
        for (Colour c : Colour.values()) {
            if (c == Colour.UNKNOWN) {
                continue;
            }
            assertTrue(rule.judge(item("a", Colour.GREY), item("b", c)).goes(), c.name());
            assertTrue(rule.judge(item("a", c), item("b", Colour.NAVY)).goes(), c.name());
        }
    }

    @Test
    void friendsGoTogetherAndOtherPairsDoNot() {
        assertTrue(rule.judge(item("a", Colour.BLUE), item("b", Colour.YELLOW)).goes());
        assertTrue(rule.judge(item("a", Colour.PINK), item("b", Colour.RED)).goes());
        assertTrue(rule.judge(item("a", Colour.YELLOW), item("b", Colour.PURPLE)).goes());
        Verdict v = rule.judge(item("a", Colour.RED), item("b", Colour.GREEN));
        assertFalse(v.goes());
        assertTrue(v.reason().contains("RED") && v.reason().contains("GREEN"), v.reason());
    }

    @Test
    void theRuleIsSymmetricOverEveryPairOfColours() {
        for (Colour ca : Colour.values()) {
            for (Colour cb : Colour.values()) {
                Item a = item("a", ca);
                Item b = item("b", cb);
                assertEquals(rule.judge(a, b).goes(), rule.judge(b, a).goes(), ca + " / " + cb);
            }
        }
    }

    @Test
    void twoDifferentItemsOfTheSameColourGoUnlessTheColourIsUnknown() {
        for (Colour c : Colour.values()) {
            boolean goes = rule.judge(item("a", c), item("b", c)).goes();
            assertEquals(c != Colour.UNKNOWN, goes, c.name());
        }
    }

    @Test
    void everyItemGoesWithItselfUnknownColourIncluded() {
        for (Colour c : Colour.values()) {
            Item a = item("a", c);
            assertTrue(rule.judge(a, a).goes(), c.name());
            assertTrue(rule.judge(a, item("a", c)).goes(), c.name() + " (same id, another instance)");
        }
    }
}
