package wardrobe.domain.outfit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
import wardrobe.domain.match.ReasonCode;
import wardrobe.domain.outfit.WhyLine.Rule;

/** One line per rule, ✓ or ✗, never an empty reason. Items are invented (no brands, no real things). */
class ExplainTest {

    static final List<Rule> ORDER = List.of(Rule.FORMALITY, Rule.COLOUR, Rule.PATTERN, Rule.SEASON, Rule.CLEAN);

    static Item item(String id, Colour colour, Pattern pattern, Formality formality, Set<Season> seasons,
            ItemStatus status) {
        return new Item(new ItemId(id), "test " + id, ItemType.ACCESSORY, colour, pattern, Fabric.COTTON, seasons,
                formality, status);
    }

    static Item plain(String id, Colour colour) {
        return item(id, colour, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.SUMMER), ItemStatus.CLEAN);
    }

    static List<WhyLine> explain(Item... items) {
        return Explain.outfit(new Outfit(List.of(items)));
    }

    static WhyLine line(List<WhyLine> lines, Rule rule) {
        return lines.stream().filter(l -> l.rule() == rule).findFirst().orElseThrow();
    }

    static List<ItemId> ids(String... ids) {
        return java.util.Arrays.stream(ids).map(ItemId::new).toList();
    }

    static void assertShape(List<WhyLine> lines) {
        assertEquals(ORDER, lines.stream().map(WhyLine::rule).toList());
        for (WhyLine l : lines) {
            assertFalse(l.reason().isBlank(), l.toString());
            assertTrue(l.code() != ReasonCode.UNCODED, l.toString());
        }
    }

    @Test
    void aGoodOutfitGetsATickForEveryRule() {
        List<WhyLine> lines = explain(plain("a", Colour.BLACK), plain("b", Colour.BLUE));
        assertShape(lines);
        lines.forEach(l -> assertTrue(l.goes(), l.toString()));
        assertEquals(ReasonCode.CLOSE_ENOUGH, line(lines, Rule.FORMALITY).code());
        assertEquals(ReasonCode.NEUTRAL, line(lines, Rule.COLOUR).code());
        assertEquals(ReasonCode.ONE_PATTERN, line(lines, Rule.PATTERN).code());
        assertEquals(ReasonCode.SHARED_SEASON, line(lines, Rule.SEASON).code());
        assertEquals(ReasonCode.ALL_CLEAN, line(lines, Rule.CLEAN).code());
        lines.forEach(l -> assertEquals(ids("a", "b"), l.items()));
    }

    @Test
    void colourTicksSayWhy() {
        assertEquals(ReasonCode.SAME_COLOUR,
                line(explain(plain("a", Colour.RED), plain("b", Colour.RED)), Rule.COLOUR).code());
        assertEquals(ReasonCode.FRIENDS,
                line(explain(plain("a", Colour.BLUE), plain("b", Colour.YELLOW)), Rule.COLOUR).code());
    }

    @Test
    void everyRuleCanFailWithItsCode() {
        Item a = item("a", Colour.RED, Pattern.STRIPED, Formality.SPORT, Set.of(Season.WINTER), ItemStatus.IN_WASH);
        Item b = item("b", Colour.GREEN, Pattern.FLORAL, Formality.FORMAL, Set.of(Season.SUMMER), ItemStatus.CLEAN);
        List<WhyLine> lines = explain(a, b);
        assertShape(lines);
        lines.forEach(l -> assertFalse(l.goes(), l.toString()));
        assertEquals(ReasonCode.STEPS_APART, line(lines, Rule.FORMALITY).code());
        assertEquals(ReasonCode.NOT_FRIENDS, line(lines, Rule.COLOUR).code());
        assertEquals(ReasonCode.TWO_PATTERNS, line(lines, Rule.PATTERN).code());
        assertEquals(ReasonCode.NO_SHARED_SEASON, line(lines, Rule.SEASON).code());
        assertEquals(ReasonCode.NOT_CLEAN, line(lines, Rule.CLEAN).code());
        assertEquals(ids("a"), line(lines, Rule.CLEAN).items());
        assertEquals(ids("a", "b"), line(lines, Rule.SEASON).items());
    }

    @Test
    void anUnknownColourIsACross() {
        WhyLine colour = line(explain(plain("a", Colour.BLACK), plain("b", Colour.UNKNOWN)), Rule.COLOUR);
        assertFalse(colour.goes());
        assertEquals(ReasonCode.UNKNOWN_COLOUR, colour.code());
    }

    @Test
    void aCrossNamesTheFirstFailingPairInInputOrder() {
        // a–b and a–c go (black is neutral); b–c (red, green) is the first pair that fails, before b–d.
        List<WhyLine> lines = explain(plain("a", Colour.BLACK), plain("b", Colour.RED), plain("c", Colour.GREEN),
                plain("d", Colour.ORANGE));
        WhyLine colour = line(lines, Rule.COLOUR);
        assertFalse(colour.goes());
        assertEquals(ids("b", "c"), colour.items());
        assertEquals(ReasonCode.NOT_FRIENDS, colour.code());
    }

    @Test
    void cleanlinessComesFromTheOutfitCheckAndNamesEveryItemThatIsNotClean() {
        Item a = item("a", Colour.BLACK, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.SUMMER), ItemStatus.IN_REPAIR);
        Item b = plain("b", Colour.WHITE);
        Item c = item("c", Colour.BLACK, Pattern.PLAIN, Formality.CASUAL, Set.of(Season.SUMMER), ItemStatus.ON_TRIP);
        Outfit outfit = new Outfit(List.of(a, b, c));
        WhyLine clean = line(Explain.outfit(outfit), Rule.CLEAN);
        assertFalse(clean.goes());
        assertEquals(ids("a", "c"), clean.items());
        List<String> expected = new ArrayList<>();
        for (OutfitProblem p : OutfitCheck.check(outfit, (x, y) -> wardrobe.domain.match.Verdict.yes("goes"))) {
            if (p instanceof OutfitProblem.NotClean) {
                expected.add(p.message());
            }
        }
        assertEquals(String.join("; ", expected), clean.reason());
    }

    @Test
    void fewerThanTwoItemsHaveNoPairsButStillOneLinePerRule() {
        for (List<WhyLine> lines : List.of(explain(), explain(plain("a", Colour.RED)))) {
            assertShape(lines);
            lines.forEach(l -> assertTrue(l.goes(), l.toString()));
            for (WhyLine l : lines.subList(0, 4)) {
                assertEquals(ReasonCode.NO_PAIRS, l.code());
            }
            assertEquals(ReasonCode.ALL_CLEAN, line(lines, Rule.CLEAN).code());
        }
    }

    @Test
    void aTickWithManyPairsJoinsTheirDistinctReasons() {
        List<WhyLine> lines = explain(plain("a", Colour.BLACK), plain("b", Colour.BLUE), plain("c", Colour.YELLOW));
        WhyLine colour = line(lines, Rule.COLOUR);
        assertTrue(colour.goes());
        assertEquals(ReasonCode.NEUTRAL, colour.code());
        assertEquals(ids("a", "b", "c"), colour.items());
        assertEquals("BLACK is neutral and goes with any colour; BLUE and YELLOW go together", colour.reason());
        // formality: every pair gives the same reason, said once
        assertEquals("formality CASUAL and CASUAL are close enough", line(lines, Rule.FORMALITY).reason());
    }

    @Test
    void everyCombinationOfSmallOutfitsGivesExactlyOneLinePerRule() {
        Colour[] colours = {Colour.BLACK, Colour.RED, Colour.GREEN, Colour.UNKNOWN};
        Pattern[] patterns = {Pattern.PLAIN, Pattern.STRIPED, Pattern.UNKNOWN};
        Formality[] formalities = {Formality.SPORT, Formality.CASUAL, Formality.FORMAL};
        ItemStatus[] statuses = ItemStatus.values();
        int n = 0;
        for (Colour c : colours) {
            for (Pattern p : patterns) {
                for (Formality f : formalities) {
                    for (ItemStatus s : statuses) {
                        Item a = item("a", c, p, f, Set.of(Season.WINTER, Season.SUMMER), s);
                        Item b = item("b", colours[n % colours.length], patterns[n % patterns.length],
                                formalities[n % formalities.length], Set.of(Season.values()[n % 4]), ItemStatus.CLEAN);
                        assertShape(explain(a, b));
                        n++;
                    }
                }
            }
        }
    }

    @Test
    void aWhyLineNeedsAReason() {
        assertThrows(IllegalArgumentException.class,
                () -> new WhyLine(Rule.CLEAN, true, ReasonCode.ALL_CLEAN, List.of(), " "));
    }
}
