package wardrobe.domain.outfit.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static wardrobe.domain.Colour.BLACK;
import static wardrobe.domain.Colour.BLUE;
import static wardrobe.domain.Colour.GREEN;
import static wardrobe.domain.Colour.RED;
import static wardrobe.domain.Colour.UNKNOWN;
import static wardrobe.domain.Colour.WHITE;
import static wardrobe.domain.Colour.YELLOW;
import static wardrobe.domain.ItemType.ACCESSORY;
import static wardrobe.domain.ItemType.BOTTOM;
import static wardrobe.domain.ItemType.DRESS;
import static wardrobe.domain.ItemType.OUTERWEAR;
import static wardrobe.domain.ItemType.SHOES;
import static wardrobe.domain.ItemType.TOP;
import static wardrobe.domain.outfit.rule.WholeItems.id;
import static wardrobe.domain.outfit.rule.WholeItems.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.outfit.rule.WholeVerdict.Check;
import wardrobe.domain.outfit.rule.WholeVerdict.Code;

class WholeRulesTest {
    private final AccentRule accents = new AccentRule();
    private final CompleteRule complete = new CompleteRule();
    private final AccessoryRule accessories = new AccessoryRule();

    private static List<Item> accessories(int n) {
        List<Item> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(item("a" + i, ACCESSORY, BLACK));
        }
        return list;
    }

    // --- accents ---

    @Test
    void allNeutralIsFewAccentsAndSaysSo() {
        WholeVerdict v = accents.judge(List.of(item("t", TOP, WHITE), item("b", BOTTOM, BLACK)));
        assertEquals(Check.ACCENTS, v.check());
        assertTrue(v.goes());
        assertEquals(Code.FEW_ACCENTS, v.code());
        assertEquals(List.of(), v.items());
        assertTrue(v.reason().contains("neutral"), v.reason());
    }

    @Test
    void exactlyTwoAccentsIsFewAccentsAndNamesThem() {
        WholeVerdict v = accents.judge(List.of(item("t", TOP, RED), item("b", BOTTOM, BLUE), item("s", SHOES, BLACK)));
        assertTrue(v.goes());
        assertEquals(Code.FEW_ACCENTS, v.code());
        assertEquals(List.of(id("t"), id("b")), v.items());
        assertTrue(v.reason().contains("red") && v.reason().contains("blue"), v.reason());
    }

    @Test
    void threeAccentsIsTooManyWithFirstItemOfEachColour() {
        WholeVerdict v = accents.judge(List.of(item("t", TOP, RED), item("b", BOTTOM, BLUE),
                item("a", ACCESSORY, RED), item("s", SHOES, GREEN)));
        assertFalse(v.goes());
        assertEquals(Code.TOO_MANY_ACCENTS, v.code());
        assertEquals(List.of(id("t"), id("b"), id("s")), v.items());
        assertTrue(v.reason().contains("red") && v.reason().contains("blue") && v.reason().contains("green"),
                v.reason());
        assertTrue(v.reason().contains(String.valueOf(AccentRule.MAX_ACCENTS)), v.reason());
    }

    @Test
    void oneColourOnTwoItemsCountsOnce() {
        WholeVerdict v = accents.judge(List.of(item("t", TOP, RED), item("b", BOTTOM, RED), item("s", SHOES, BLUE),
                item("a", ACCESSORY, BLUE)));
        assertTrue(v.goes());
        assertEquals(Code.FEW_ACCENTS, v.code());
    }

    @Test
    void unknownColourIsNoWithThoseItems() {
        WholeVerdict v = accents.judge(List.of(item("t", TOP, UNKNOWN), item("b", BOTTOM, RED),
                item("s", SHOES, UNKNOWN)));
        assertFalse(v.goes());
        assertEquals(Code.UNKNOWN_COLOUR, v.code());
        assertEquals(List.of(id("t"), id("s")), v.items());
    }

    @Test
    void unknownWinsOverTooManyAccents() {
        WholeVerdict v = accents.judge(List.of(item("t", TOP, RED), item("b", BOTTOM, BLUE),
                item("s", SHOES, GREEN), item("o", OUTERWEAR, UNKNOWN)));
        assertEquals(Code.UNKNOWN_COLOUR, v.code());
        assertEquals(List.of(id("o")), v.items());
    }

    @Test
    void accentsAreDistinctNonNeutralInInputOrderWithoutUnknown() {
        List<Colour> found = AccentRule.accents(List.of(item("1", TOP, YELLOW), item("2", BOTTOM, BLACK),
                item("3", SHOES, UNKNOWN), item("4", ACCESSORY, RED), item("5", OUTERWEAR, YELLOW)));
        assertEquals(List.of(YELLOW, RED), found);
        assertEquals(List.of(), AccentRule.accents(List.of()));
    }

    @Test
    void maxAccentsIsTwo() {
        assertEquals(2, AccentRule.MAX_ACCENTS);
    }

    // --- complete ---

    @Test
    void topBottomShoesIsComplete() {
        WholeVerdict v = complete.judge(List.of(item("a", ACCESSORY, BLACK), item("s", SHOES, BLACK),
                item("b", BOTTOM, BLACK), item("t", TOP, WHITE)));
        assertEquals(Check.COMPLETE, v.check());
        assertTrue(v.goes());
        assertEquals(Code.COMPLETE, v.code());
        assertEquals(List.of(id("t"), id("b"), id("s")), v.items());
    }

    @Test
    void dressAndShoesIsComplete() {
        WholeVerdict v = complete.judge(List.of(item("d", DRESS, RED), item("s", SHOES, BLACK)));
        assertTrue(v.goes());
        assertEquals(Code.COMPLETE, v.code());
        assertEquals(List.of(id("d"), id("s")), v.items());
    }

    @Test
    void dressWithATopAndNoBottomIsComplete() {
        WholeVerdict v = complete.judge(List.of(item("t", TOP, WHITE), item("d", DRESS, RED), item("s", SHOES, BLACK)));
        assertEquals(Code.COMPLETE, v.code());
        assertEquals(List.of(id("d"), id("s")), v.items());
    }

    @Test
    void noBodyIsMissingBody() {
        WholeVerdict v = complete.judge(List.of(item("s", SHOES, BLACK), item("o", OUTERWEAR, BLACK)));
        assertFalse(v.goes());
        assertEquals(Code.MISSING_BODY, v.code());
        assertEquals(List.of(), v.items());
    }

    @Test
    void bottomWithoutTopIsMissingTop() {
        WholeVerdict v = complete.judge(List.of(item("b", BOTTOM, BLACK), item("s", SHOES, BLACK)));
        assertFalse(v.goes());
        assertEquals(Code.MISSING_TOP, v.code());
        assertEquals("no top", v.reason());
    }

    @Test
    void topWithoutBottomIsMissingBottom() {
        WholeVerdict v = complete.judge(List.of(item("t", TOP, BLACK), item("s", SHOES, BLACK)));
        assertFalse(v.goes());
        assertEquals(Code.MISSING_BOTTOM, v.code());
        assertEquals(List.of(), v.items());
    }

    @Test
    void bodyWithoutShoesIsMissingShoes() {
        WholeVerdict v = complete.judge(List.of(item("t", TOP, BLACK), item("b", BOTTOM, BLACK)));
        assertFalse(v.goes());
        assertEquals(Code.MISSING_SHOES, v.code());
        assertEquals("no shoes", v.reason());
    }

    @Test
    void firstMissingGivesTheCodeAndTheReasonNamesAll() {
        WholeVerdict v = complete.judge(List.of(item("t", TOP, BLACK)));
        assertEquals(Code.MISSING_BOTTOM, v.code());
        assertEquals("no bottom and no shoes", v.reason());
        WholeVerdict none = complete.judge(List.of(item("a", ACCESSORY, BLACK)));
        assertEquals(Code.MISSING_BODY, none.code());
        assertTrue(none.reason().endsWith(" and no shoes"), none.reason());
    }

    // --- accessories ---

    @Test
    void exactlyThreeAccessoriesIsFew() {
        List<Item> outfit = accessories(3);
        WholeVerdict v = accessories.judge(outfit);
        assertEquals(Check.ACCESSORIES, v.check());
        assertTrue(v.goes());
        assertEquals(Code.FEW_ACCESSORIES, v.code());
    }

    @Test
    void fourAccessoriesIsTooManyWithAllOfThem() {
        List<Item> outfit = new ArrayList<>(accessories(4));
        outfit.add(1, item("t", TOP, WHITE));
        WholeVerdict v = accessories.judge(outfit);
        assertFalse(v.goes());
        assertEquals(Code.TOO_MANY_ACCESSORIES, v.code());
        assertEquals(List.of(id("a0"), id("a1"), id("a2"), id("a3")), v.items());
        assertTrue(v.reason().contains(String.valueOf(AccessoryRule.MAX_ACCESSORIES)), v.reason());
    }

    @Test
    void maxAccessoriesIsThree() {
        assertEquals(3, AccessoryRule.MAX_ACCESSORIES);
    }

    // --- all rules ---

    @Test
    void standardIsAccentCompleteAccessoryInThatOrder() {
        List<WholeRule> rules = WholeRules.standard();
        assertEquals(3, rules.size());
        assertTrue(rules.get(0) instanceof AccentRule);
        assertTrue(rules.get(1) instanceof CompleteRule);
        assertTrue(rules.get(2) instanceof AccessoryRule);
    }

    @Test
    void judgeAllGivesOneVerdictPerRuleInOrder() {
        List<WholeVerdict> all = WholeRules.judgeAll(List.of(item("t", TOP, RED), item("b", BOTTOM, BLACK),
                item("s", SHOES, BLACK)));
        assertEquals(List.of(Check.ACCENTS, Check.COMPLETE, Check.ACCESSORIES),
                all.stream().map(WholeVerdict::check).toList());
        assertEquals(List.of(Code.FEW_ACCENTS, Code.COMPLETE, Code.FEW_ACCESSORIES),
                all.stream().map(WholeVerdict::code).toList());
    }

    @Test
    void emptyOutfit() {
        List<WholeVerdict> all = WholeRules.judgeAll(List.of());
        assertEquals(List.of(Code.FEW_ACCENTS, Code.MISSING_BODY, Code.FEW_ACCESSORIES),
                all.stream().map(WholeVerdict::code).toList());
    }

    @Test
    void goesAndCodeDoNotDependOnOrder() {
        List<Item> outfit = List.of(item("t", TOP, RED), item("b", BOTTOM, BLUE), item("s", SHOES, GREEN),
                item("a0", ACCESSORY, BLACK), item("a1", ACCESSORY, WHITE), item("a2", ACCESSORY, RED),
                item("a3", ACCESSORY, YELLOW), item("o", OUTERWEAR, BLACK));
        List<WholeVerdict> base = WholeRules.judgeAll(outfit);
        Random rnd = new Random(39L);
        for (int i = 0; i < 20; i++) {
            List<Item> shuffled = new ArrayList<>(outfit);
            java.util.Collections.shuffle(shuffled, rnd);
            List<WholeVerdict> again = WholeRules.judgeAll(shuffled);
            for (int r = 0; r < base.size(); r++) {
                assertEquals(base.get(r).goes(), again.get(r).goes(), "shuffle " + i + " rule " + r);
                assertEquals(base.get(r).code(), again.get(r).code(), "shuffle " + i + " rule " + r);
            }
        }
    }

    @Test
    void everyCodeIsReachedByTheseTests() {
        Set<Code> seen = EnumSet.noneOf(Code.class);
        List<List<Item>> outfits = List.of(
                List.of(),
                List.of(item("t", TOP, RED), item("b", BOTTOM, BLUE), item("s", SHOES, GREEN)),
                List.of(item("t", TOP, UNKNOWN)),
                List.of(item("d", DRESS, BLACK), item("s", SHOES, BLACK)),
                List.of(item("b", BOTTOM, BLACK)),
                List.of(item("t", TOP, BLACK)),
                List.of(item("t", TOP, BLACK), item("b", BOTTOM, BLACK)),
                accessories(4));
        for (List<Item> outfit : outfits) {
            WholeRules.judgeAll(outfit).forEach(v -> seen.add(v.code()));
        }
        assertEquals(EnumSet.allOf(Code.class), seen,
                "missing: " + Arrays.stream(Code.values()).filter(c -> !seen.contains(c))
                        .map(Enum::name).collect(Collectors.joining(", ")));
    }

    // --- verdict ---

    @Test
    void verdictNeedsAReasonAndCopiesItems() {
        assertThrows(IllegalArgumentException.class,
                () -> new WholeVerdict(Check.ACCENTS, true, Code.FEW_ACCENTS, List.of(), " "));
        assertThrows(IllegalArgumentException.class,
                () -> new WholeVerdict(Check.ACCENTS, true, Code.FEW_ACCENTS, List.of(), null));
        List<ItemId> ids = new ArrayList<>(List.of(id("x")));
        WholeVerdict v = new WholeVerdict(Check.ACCENTS, true, Code.FEW_ACCENTS, ids, "fine");
        ids.add(id("y"));
        assertEquals(List.of(id("x")), v.items());
    }

    @Test
    void everyVerdictHasAReason() {
        for (WholeVerdict v : WholeRules.judgeAll(List.of(item("t", TOP, UNKNOWN)))) {
            assertFalse(v.reason().isBlank());
        }
    }
}
