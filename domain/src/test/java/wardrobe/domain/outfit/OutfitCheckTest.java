package wardrobe.domain.outfit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import wardrobe.domain.match.CombinationRule;
import wardrobe.domain.match.Verdict;
import wardrobe.domain.outfit.OutfitProblem.Clash;
import wardrobe.domain.outfit.OutfitProblem.DressWithSeparates;
import wardrobe.domain.outfit.OutfitProblem.Empty;
import wardrobe.domain.outfit.OutfitProblem.NotClean;
import wardrobe.domain.outfit.OutfitProblem.TwoOfAKind;

/** One test per kind of problem, plus their order. Items are invented (no brands, no real things). */
class OutfitCheckTest {

    static final CombinationRule ALL_GO = (a, b) -> Verdict.yes("goes");

    static Item item(String id, ItemType type) {
        return item(id, type, ItemStatus.CLEAN);
    }

    static Item item(String id, ItemType type, ItemStatus status) {
        return new Item(new ItemId(id), "test " + id, type, Colour.BLACK, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, status);
    }

    static List<OutfitProblem> check(CombinationRule rule, Item... items) {
        return OutfitCheck.check(new Outfit(List.of(items)), rule);
    }

    @Test
    void aCleanOutfitWithOneItemPerSlotIsFine() {
        List<OutfitProblem> problems = check(ALL_GO, item("t", ItemType.TOP), item("b", ItemType.BOTTOM),
                item("o", ItemType.OUTERWEAR), item("s", ItemType.SHOES), item("x", ItemType.ACCESSORY),
                item("y", ItemType.ACCESSORY), item("z", ItemType.ACCESSORY));
        assertEquals(List.of(), problems);
    }

    @Test
    void aDressOutfitIsFine() {
        assertEquals(List.of(), check(ALL_GO, item("d", ItemType.DRESS), item("s", ItemType.SHOES)));
    }

    @Test
    void noBottomAndNoDressIsNotAProblemInVersionOne() {
        assertEquals(List.of(), check(ALL_GO, item("t", ItemType.TOP), item("s", ItemType.SHOES)));
    }

    @Test
    void anItemThatIsNotCleanIsAProblem() {
        for (ItemStatus status : ItemStatus.values()) {
            Item t = item("t", ItemType.TOP, status);
            List<OutfitProblem> problems = check(ALL_GO, t, item("b", ItemType.BOTTOM));
            if (status == ItemStatus.CLEAN) {
                assertEquals(List.of(), problems);
            } else {
                assertEquals(List.of(new NotClean(t)), problems, status.name());
                assertTrue(problems.get(0).message().contains("test t"));
            }
        }
    }

    @Test
    void twoOfASlotTypeIsAProblemButAccessoriesAreNot() {
        for (ItemType type : ItemType.values()) {
            Item a = item("a", type);
            Item b = item("b", type);
            List<OutfitProblem> problems = check(ALL_GO, a, b);
            if (type == ItemType.ACCESSORY) {
                assertEquals(List.of(), problems);
            } else {
                assertEquals(List.of(new TwoOfAKind(type, a, b)), problems, type.name());
            }
        }
    }

    @Test
    void aThirdOfAKindIsReportedAgainstTheFirst() {
        Item a = item("a", ItemType.SHOES);
        Item b = item("b", ItemType.SHOES);
        Item c = item("c", ItemType.SHOES);
        assertEquals(List.of(new TwoOfAKind(ItemType.SHOES, a, b), new TwoOfAKind(ItemType.SHOES, a, c)),
                check(ALL_GO, a, b, c));
    }

    @Test
    void aDressWithATopOrABottomIsAProblem() {
        Item d = item("d", ItemType.DRESS);
        Item t = item("t", ItemType.TOP);
        Item b = item("b", ItemType.BOTTOM);
        assertEquals(List.of(new DressWithSeparates(d, t), new DressWithSeparates(d, b)),
                check(ALL_GO, t, d, item("o", ItemType.OUTERWEAR), b));
    }

    @Test
    void everyPairThatDoesNotGoIsAClashInInputOrder() {
        Item a = item("a", ItemType.TOP);
        Item b = item("b", ItemType.BOTTOM);
        Item c = item("c", ItemType.SHOES);
        CombinationRule notWithC = (x, y) -> x == c || y == c ? Verdict.no("c goes with nothing")
                : Verdict.yes("goes");
        List<OutfitProblem> problems = check(notWithC, a, b, c);
        assertEquals(List.of(new Clash(a, c, "c goes with nothing"), new Clash(b, c, "c goes with nothing")),
                problems);
        assertTrue(problems.get(0).message().contains("c goes with nothing"));
    }

    @Test
    void anEmptyOutfitIsAProblem() {
        List<OutfitProblem> problems = check(ALL_GO);
        assertEquals(List.of(new Empty()), problems);
        assertFalse(problems.get(0).message().isBlank());
    }

    @Test
    void problemsComeInTheOrderOfTheirKinds() {
        Item dirty = item("w", ItemType.TOP, ItemStatus.IN_WASH);
        Item top = item("t", ItemType.TOP);
        Item dress = item("d", ItemType.DRESS);
        CombinationRule neverGoes = (x, y) -> Verdict.no("no");
        List<OutfitProblem> problems = check(neverGoes, dirty, top, dress);
        assertEquals(List.of(
                new NotClean(dirty),
                new TwoOfAKind(ItemType.TOP, dirty, top),
                new DressWithSeparates(dress, dirty),
                new DressWithSeparates(dress, top),
                new Clash(dirty, top, "no"),
                new Clash(dirty, dress, "no"),
                new Clash(top, dress, "no")), problems);
    }

    @Test
    void everyProblemHasAMessage() {
        Item a = item("a", ItemType.TOP);
        Item b = item("b", ItemType.DRESS);
        List<OutfitProblem> all = List.of(new NotClean(a), new TwoOfAKind(ItemType.TOP, a, a),
                new DressWithSeparates(b, a), new Clash(a, b, "why"), new Empty());
        for (OutfitProblem p : all) {
            assertFalse(p.message().isBlank(), p.toString());
        }
    }
}
