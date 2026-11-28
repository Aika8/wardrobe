package wardrobe.domain.occasion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;
import wardrobe.domain.occasion.OccasionVerdict.Code;

class OccasionTest {

    /** The whole table, each cell written out: occasion, formality, allowed. */
    static Stream<Arguments> table() {
        return Stream.of(
                Arguments.of(Occasion.OFFICE, Formality.SPORT, false),
                Arguments.of(Occasion.OFFICE, Formality.CASUAL, false),
                Arguments.of(Occasion.OFFICE, Formality.SMART_CASUAL, true),
                Arguments.of(Occasion.OFFICE, Formality.BUSINESS, true),
                Arguments.of(Occasion.OFFICE, Formality.FORMAL, false),
                Arguments.of(Occasion.WEEKEND, Formality.SPORT, false),
                Arguments.of(Occasion.WEEKEND, Formality.CASUAL, true),
                Arguments.of(Occasion.WEEKEND, Formality.SMART_CASUAL, true),
                Arguments.of(Occasion.WEEKEND, Formality.BUSINESS, false),
                Arguments.of(Occasion.WEEKEND, Formality.FORMAL, false),
                Arguments.of(Occasion.EVENING, Formality.SPORT, false),
                Arguments.of(Occasion.EVENING, Formality.CASUAL, false),
                Arguments.of(Occasion.EVENING, Formality.SMART_CASUAL, true),
                Arguments.of(Occasion.EVENING, Formality.BUSINESS, true),
                Arguments.of(Occasion.EVENING, Formality.FORMAL, true),
                Arguments.of(Occasion.SPORT, Formality.SPORT, true),
                Arguments.of(Occasion.SPORT, Formality.CASUAL, true),
                Arguments.of(Occasion.SPORT, Formality.SMART_CASUAL, false),
                Arguments.of(Occasion.SPORT, Formality.BUSINESS, false),
                Arguments.of(Occasion.SPORT, Formality.FORMAL, false));
    }

    @ParameterizedTest(name = "{0} allows {1}: {2}")
    @MethodSource("table")
    void tableCell(Occasion occasion, Formality formality, boolean allowed) {
        assertEquals(allowed, occasion.allows(formality));
    }

    @Test
    void tableCoversEveryCell() {
        assertEquals(Occasion.values().length * Formality.values().length, table().count());
        assertEquals(4, Occasion.values().length);
        assertEquals(5, Formality.values().length);
    }

    @ParameterizedTest(name = "{0} one-item verdict for {1} agrees with allows")
    @MethodSource("table")
    void judgeAgreesWithTable(Occasion occasion, Formality formality, boolean allowed) {
        OccasionVerdict verdict = OccasionCheck.judge(List.of(item("x", formality)), occasion);
        assertEquals(allowed, verdict.goes());
        assertFalse(verdict.reason().isBlank());
    }

    @Test
    void fitsOccasion() {
        OccasionVerdict verdict = OccasionCheck.judge(
                List.of(item("shirt", Formality.BUSINESS), item("trousers", Formality.SMART_CASUAL),
                        item("loafers", Formality.BUSINESS), item("watch", Formality.SMART_CASUAL)),
                Occasion.OFFICE);
        assertTrue(verdict.goes());
        assertEquals(Code.FITS_OCCASION, verdict.code());
        assertEquals(List.of(), verdict.items());
        assertEquals("everything is between SMART_CASUAL and BUSINESS, as OFFICE asks", verdict.reason());
    }

    @Test
    void tooCasual() {
        OccasionVerdict verdict = OccasionCheck.judge(
                List.of(item("shirt", Formality.BUSINESS), item("sneakers", Formality.SPORT)), Occasion.OFFICE);
        assertFalse(verdict.goes());
        assertEquals(Code.TOO_CASUAL, verdict.code());
        assertEquals(List.of(new ItemId("sneakers")), verdict.items());
        assertTrue(verdict.reason().contains("sneakers"), verdict.reason());
        assertTrue(verdict.reason().contains("SPORT"), verdict.reason());
        assertTrue(verdict.reason().contains("SMART_CASUAL and BUSINESS"), verdict.reason());
    }

    @Test
    void tooFormal() {
        OccasionVerdict verdict = OccasionCheck.judge(
                List.of(item("tee", Formality.CASUAL), item("gown", Formality.FORMAL)), Occasion.WEEKEND);
        assertFalse(verdict.goes());
        assertEquals(Code.TOO_FORMAL, verdict.code());
        assertEquals(List.of(new ItemId("gown")), verdict.items());
        assertTrue(verdict.reason().contains("gown"), verdict.reason());
        assertTrue(verdict.reason().contains("FORMAL"), verdict.reason());
        assertTrue(verdict.reason().contains("CASUAL and SMART_CASUAL"), verdict.reason());
    }

    @Test
    void tooCasualWinsAndListsBothSidesInInputOrder() {
        OccasionVerdict verdict = OccasionCheck.judge(
                List.of(item("gown", Formality.FORMAL), item("shirt", Formality.BUSINESS),
                        item("hoodie", Formality.SPORT)),
                Occasion.OFFICE);
        assertFalse(verdict.goes());
        assertEquals(Code.TOO_CASUAL, verdict.code());
        assertEquals(List.of(new ItemId("gown"), new ItemId("hoodie")), verdict.items());
        assertTrue(verdict.reason().contains("gown"), verdict.reason());
        assertTrue(verdict.reason().contains("hoodie"), verdict.reason());
    }

    @Test
    void shoesAndAccessoriesCount() {
        Item shoes = new Item(new ItemId("flip-flops"), "flip-flops", ItemType.SHOES, Colour.BLACK, Pattern.PLAIN,
                Fabric.SYNTHETIC, EnumSet.of(Season.SUMMER), Formality.SPORT, ItemStatus.CLEAN);
        Item bag = new Item(new ItemId("clutch"), "clutch", ItemType.ACCESSORY, Colour.BLACK, Pattern.PLAIN,
                Fabric.LEATHER, EnumSet.of(Season.SUMMER), Formality.FORMAL, ItemStatus.CLEAN);
        assertEquals(Code.TOO_CASUAL, OccasionCheck.judge(List.of(item("shirt", Formality.BUSINESS), shoes),
                Occasion.OFFICE).code());
        assertEquals(Code.TOO_FORMAL, OccasionCheck.judge(List.of(item("tee", Formality.CASUAL), bag),
                Occasion.SPORT).code());
    }

    @Test
    void emptyListFits() {
        for (Occasion occasion : Occasion.values()) {
            OccasionVerdict verdict = OccasionCheck.judge(List.of(), occasion);
            assertTrue(verdict.goes());
            assertEquals(Code.FITS_OCCASION, verdict.code());
            assertEquals(List.of(), verdict.items());
            assertEquals("nothing to judge", verdict.reason());
        }
    }

    @Test
    void reasonsNeverBlank() {
        for (Occasion occasion : Occasion.values()) {
            for (Formality a : Formality.values()) {
                for (Formality b : Formality.values()) {
                    OccasionVerdict verdict = OccasionCheck.judge(List.of(item("a", a), item("b", b)), occasion);
                    assertFalse(verdict.reason().isBlank());
                    assertEquals(verdict.goes(), verdict.code() == Code.FITS_OCCASION);
                }
            }
        }
    }

    private static Item item(String id, Formality formality) {
        return new Item(new ItemId(id), id, ItemType.TOP, Colour.WHITE, Pattern.PLAIN, Fabric.COTTON,
                EnumSet.of(Season.SUMMER), formality, ItemStatus.CLEAN);
    }
}
