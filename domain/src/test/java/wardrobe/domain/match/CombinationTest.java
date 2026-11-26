package wardrobe.domain.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/** Intent 01, invariant 3; the standard rules in a fixed order. Items are invented (no brands, no real things). */
class CombinationTest {

    final Combination standard = Combination.standard();

    static Item item(String id, Colour colour, Pattern pattern, Formality formality, Set<Season> seasons) {
        return new Item(new ItemId(id), "test " + id, ItemType.TOP, colour, pattern, Fabric.COTTON, seasons,
                formality, ItemStatus.CLEAN);
    }

    @Test
    void aPairEveryRuleLikesGoesAndNamesTheRulesInOrder() {
        Item a = item("a", Colour.BLACK, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.SUMMER));
        Item b = item("b", Colour.RED, Pattern.STRIPED, Formality.SMART_CASUAL, Set.of(Season.SUMMER));
        Verdict v = standard.judge(a, b);
        assertTrue(v.goes(), v.reason());
        assertEquals("goes: formality, colour, pattern, season", v.reason());
    }

    @Test
    void aPairFailingTwoRulesShowsBothReasonsInRuleOrder() {
        Item a = item("a", Colour.RED, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.WINTER));
        Item b = item("b", Colour.GREEN, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.SUMMER));
        Verdict v = standard.judge(a, b);
        assertFalse(v.goes());
        String colour = new ColourRule().judge(a, b).reason();
        String season = new SeasonRule().judge(a, b).reason();
        assertEquals(colour + "; " + season, v.reason());
    }

    @Test
    void allFourFailingReasonsComeInFormalityColourPatternSeasonOrder() {
        Item a = item("a", Colour.RED, Pattern.FLORAL, Formality.SPORT, Set.of(Season.WINTER));
        Item b = item("b", Colour.GREEN, Pattern.CHECKED, Formality.FORMAL, Set.of(Season.SUMMER));
        Verdict v = standard.judge(a, b);
        assertFalse(v.goes());
        List<String> expected = List.of(new FormalityRule().judge(a, b).reason(), new ColourRule().judge(a, b).reason(),
                new PatternRule().judge(a, b).reason(), new SeasonRule().judge(a, b).reason());
        assertEquals(String.join("; ", expected), v.reason());
    }

    @Test
    void theOrderOfTheListIsTheOrderOfTheReasons() {
        Item a = item("a", Colour.RED, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.WINTER));
        Item b = item("b", Colour.GREEN, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.SUMMER));
        Verdict v = new Combination(List.of(new SeasonRule(), new ColourRule())).judge(a, b);
        assertEquals(new SeasonRule().judge(a, b).reason() + "; " + new ColourRule().judge(a, b).reason(),
                v.reason());
        Verdict yes = new Combination(List.of(new SeasonRule(), new FormalityRule())).judge(a, a);
        assertEquals("goes: season, formality", yes.reason());
    }

    @Test
    void aCombinationNeedsAtLeastOneRule() {
        assertThrows(IllegalArgumentException.class, () -> new Combination(List.of()));
    }

    /** At least 20 invented items with varied colour, pattern, formality and seasons, unknowns included. */
    static List<Item> variedItems() {
        Colour[] colours = Colour.values();
        Pattern[] patterns = Pattern.values();
        Formality[] formalities = Formality.values();
        Season[] seasons = Season.values();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            Set<Season> s = EnumSet.of(seasons[i % seasons.length]);
            if (i % 3 == 0) {
                s.add(seasons[(i / 3) % seasons.length]);
            }
            if (i % 7 == 0) {
                s = EnumSet.allOf(Season.class);
            }
            items.add(item("i" + i, colours[(i * 5) % colours.length], patterns[(i * 3) % patterns.length],
                    formalities[(i * 2) % formalities.length], s));
        }
        return items;
    }

    @Test
    void theStandardCombinationIsSymmetricAndAnItemGoesWithItself() {
        List<Item> items = variedItems();
        assertTrue(items.size() >= 20);
        int yes = 0;
        int no = 0;
        for (Item a : items) {
            assertTrue(standard.judge(a, a).goes(), a.toString());
            for (Item b : items) {
                Verdict ab = standard.judge(a, b);
                Verdict ba = standard.judge(b, a);
                assertEquals(ab.goes(), ba.goes(), a + " / " + b);
                if (ab.goes()) {
                    yes++;
                } else {
                    no++;
                }
            }
        }
        assertTrue(yes > items.size(), "some different items go together: " + yes);
        assertTrue(no > 0, "some pairs do not go");
    }

    @Test
    void theItemSetVariesEveryField() {
        List<Item> items = variedItems();
        assertTrue(items.stream().map(Item::colour).distinct().count() >= 10);
        assertTrue(items.stream().anyMatch(i -> i.colour() == Colour.UNKNOWN));
        assertEquals(Pattern.values().length, items.stream().map(Item::pattern).distinct().count());
        assertEquals(Formality.values().length, items.stream().map(Item::formality).distinct().count());
        assertTrue(items.stream().map(Item::seasons).distinct().count() >= 5);
    }
}
