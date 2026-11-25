package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.match.ColourItems.item;

import java.util.List;
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

    /** The starting table from the task, written out here on purpose: a dropped or mistyped row in FRIENDS fails. */
    static final List<List<Colour>> EXPECTED_FRIENDS = List.of(
            List.of(Colour.BLUE, Colour.YELLOW),
            List.of(Colour.BLUE, Colour.ORANGE),
            List.of(Colour.BLUE, Colour.PINK),
            List.of(Colour.GREEN, Colour.PINK),
            List.of(Colour.PURPLE, Colour.YELLOW),
            List.of(Colour.RED, Colour.PINK));

    @Test
    void everyFriendPairGoesTogetherInBothOrders() {
        for (List<Colour> pair : EXPECTED_FRIENDS) {
            Colour x = pair.get(0);
            Colour y = pair.get(1);
            assertTrue(rule.judge(item("a", x), item("b", y)).goes(), x + " / " + y);
            assertTrue(rule.judge(item("a", y), item("b", x)).goes(), y + " / " + x);
        }
    }

    @Test
    void twoDifferentNonNeutralColoursGoOnlyWhenTheyAreFriends() {
        for (Colour x : Colour.values()) {
            for (Colour y : Colour.values()) {
                if (x == y || x == Colour.UNKNOWN || y == Colour.UNKNOWN || x.neutral() || y.neutral()) {
                    continue;
                }
                boolean friends = EXPECTED_FRIENDS.contains(List.of(x, y)) || EXPECTED_FRIENDS.contains(List.of(y, x));
                Verdict v = rule.judge(item("a", x), item("b", y));
                assertEquals(friends, v.goes(), x + " / " + y);
                if (!friends) {
                    assertTrue(v.reason().contains(x.name()) && v.reason().contains(y.name()), v.reason());
                }
            }
        }
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
