package wardrobe.app.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;
import wardrobe.domain.match.ColourRule;
import wardrobe.domain.match.ReasonCode;

/** Pins the shape of the invented 60-item wardrobe (WR-038). Plain JUnit: no Spring context. */
class SampleWardrobeTest {

    private static final List<Item> ITEMS = SampleWardrobe.items();

    @Test
    void exactlySixtyItemsInAnImmutableStableList() {
        assertEquals(60, ITEMS.size());
        assertEquals(ITEMS, SampleWardrobe.items());
        assertThrows(UnsupportedOperationException.class, () -> ITEMS.add(ITEMS.get(0)));
    }

    @Test
    void idsAreUniqueAndKebabCase() {
        Set<String> ids = new HashSet<>();
        for (Item item : ITEMS) {
            String id = item.id().value();
            assertTrue(id.matches("[a-z0-9]+(-[a-z0-9]+)*"), "not kebab-case: " + id);
            assertTrue(ids.add(id), "duplicate id: " + id);
        }
    }

    @Test
    void groupedByTypeOrder() {
        for (int i = 1; i < ITEMS.size(); i++) {
            assertTrue(ITEMS.get(i - 1).type().ordinal() <= ITEMS.get(i).type().ordinal(),
                    "out of type order at " + ITEMS.get(i).id());
        }
    }

    @Test
    void enoughOfEveryType() {
        assertAtLeast(14, ItemType.TOP);
        assertAtLeast(10, ItemType.BOTTOM);
        assertAtLeast(6, ItemType.DRESS);
        assertAtLeast(7, ItemType.OUTERWEAR);
        assertAtLeast(9, ItemType.SHOES);
        assertAtLeast(8, ItemType.ACCESSORY);
    }

    @Test
    void mostlyCleanAndSomeOfEveryOtherStatus() {
        assertTrue(count(i -> i.status() == ItemStatus.CLEAN) >= 44, "clean items");
        for (ItemStatus status : EnumSet.complementOf(EnumSet.of(ItemStatus.CLEAN))) {
            assertTrue(count(i -> i.status() == status) >= 3, "items " + status);
        }
    }

    @Test
    void everyKnownColourPatternAndFabricAppears() {
        assertEquals(known(Colour.values(), Colour.UNKNOWN), used(Item::colour));
        assertEquals(known(Pattern.values(), Pattern.UNKNOWN), used(Item::pattern));
        assertEquals(known(Fabric.values(), Fabric.UNKNOWN), used(Item::fabric));
    }

    @Test
    void everyFormalityAndSeasonIsWellCovered() {
        for (Formality formality : Formality.values()) {
            assertTrue(count(i -> i.formality() == formality) >= 3, "formality " + formality);
        }
        for (Season season : Season.values()) {
            assertTrue(count(i -> i.seasons().contains(season)) >= 15, "season " + season);
        }
    }

    @Test
    void oneUnknownColourPatternAndFabricOnThreeDifferentItemsNoneClean() {
        List<Item> colour = ITEMS.stream().filter(i -> i.colour() == Colour.UNKNOWN).toList();
        List<Item> pattern = ITEMS.stream().filter(i -> i.pattern() == Pattern.UNKNOWN).toList();
        List<Item> fabric = ITEMS.stream().filter(i -> i.fabric() == Fabric.UNKNOWN).toList();
        assertEquals(1, colour.size(), "unknown colour");
        assertEquals(1, pattern.size(), "unknown pattern");
        assertEquals(1, fabric.size(), "unknown fabric");
        assertEquals(3, Set.of(colour.get(0).id(), pattern.get(0).id(), fabric.get(0).id()).size());
        for (Item item : List.of(colour.get(0), pattern.get(0), fabric.get(0))) {
            assertNotEquals(ItemStatus.CLEAN, item.status(), "unknown field on a clean item " + item.id());
        }
    }

    @Test
    void everyItemBelongsToOneCapsuleOfItsFormalities() {
        for (Item item : ITEMS) {
            Capsule capsule = Capsule.of(item);
            assertTrue(capsule.formalities.contains(item.formality()),
                    item.id() + " is " + item.formality() + ", not of capsule " + capsule);
        }
    }

    @Test
    void officeCapsule() {
        List<Item> c = capsule(Capsule.OFFICE);
        assertTrue(count(c, type(ItemType.TOP)) >= 4, "office tops");
        assertTrue(count(c, type(ItemType.TOP).and(fabric(Fabric.COTTON, Fabric.SILK))) >= 2, "cotton/silk shirts");
        assertTrue(count(c, type(ItemType.TOP).and(fabric(Fabric.WOOL))) >= 1, "wool knit");
        assertTrue(count(c, type(ItemType.BOTTOM)) >= 3, "office bottoms");
        assertTrue(count(c, type(ItemType.BOTTOM).and(fabric(Fabric.WOOL)).and(named("trousers"))) >= 1, "trousers");
        assertTrue(count(c, type(ItemType.BOTTOM).and(named("skirt"))) >= 1, "skirt");
        assertTrue(count(c, type(ItemType.DRESS)) >= 2, "office dresses");
        assertTrue(count(c, type(ItemType.SHOES).and(fabric(Fabric.LEATHER))) >= 2, "leather shoes");
        assertTrue(count(c, type(ItemType.SHOES).and(fabric(Fabric.LEATHER)).and(in(Season.WINTER))) >= 1,
                "winter leather shoes");
        assertTrue(count(c, type(ItemType.OUTERWEAR)) >= 2, "office outerwear");
        assertTrue(count(c, type(ItemType.OUTERWEAR).and(fabric(Fabric.WOOL)).and(named("coat"))
                .and(in(Season.AUTUMN, Season.WINTER))) >= 1, "wool coat");
        assertTrue(count(c, type(ItemType.OUTERWEAR).and(fabric(Fabric.COTTON)).and(named("trench"))
                .and(in(Season.SPRING, Season.AUTUMN))) >= 1, "cotton trench");
    }

    @Test
    void weekendCapsule() {
        List<Item> c = capsule(Capsule.WEEKEND);
        assertTrue(count(c, type(ItemType.TOP)) >= 4, "weekend tops");
        assertTrue(count(c, type(ItemType.TOP).and(i -> i.pattern() == Pattern.STRIPED)) >= 1, "striped top");
        assertTrue(count(c, type(ItemType.TOP).and(i -> i.pattern() == Pattern.FLORAL)) >= 1, "floral top");
        assertTrue(count(c, type(ItemType.BOTTOM)) >= 3, "weekend bottoms");
        assertTrue(count(c, type(ItemType.BOTTOM).and(fabric(Fabric.DENIM)).and(named("jeans"))) >= 1, "jeans");
        assertTrue(count(c, type(ItemType.BOTTOM).and(named("chinos"))) >= 1, "chinos");
        assertTrue(count(c, type(ItemType.DRESS)) >= 2, "weekend dresses");
        assertTrue(count(c, type(ItemType.DRESS).and(fabric(Fabric.LINEN)).and(in(Season.SUMMER))) >= 1,
                "summer linen dress");
        assertTrue(count(c, type(ItemType.SHOES)) >= 2, "weekend shoes");
        assertTrue(count(c, type(ItemType.SHOES).and(named("sneakers"))) >= 1, "sneakers");
        assertTrue(count(c, type(ItemType.SHOES).and(named("boots")).and(in(Season.WINTER))) >= 1, "winter boots");
        assertTrue(count(c, type(ItemType.OUTERWEAR)) >= 2, "weekend outerwear");
        assertTrue(count(c, type(ItemType.OUTERWEAR).and(fabric(Fabric.SYNTHETIC)).and(named("puffer"))
                .and(in(Season.WINTER))) >= 1, "puffer");
        assertTrue(count(c, type(ItemType.OUTERWEAR).and(fabric(Fabric.DENIM)).and(named("jacket"))
                .and(in(Season.SPRING, Season.SUMMER, Season.AUTUMN))) >= 1, "denim jacket");
    }

    @Test
    void sportCapsule() {
        List<Item> c = capsule(Capsule.SPORT);
        assertTrue(count(c, type(ItemType.TOP)) >= 2, "sport tops");
        assertTrue(count(c, type(ItemType.BOTTOM)) >= 2, "sport bottoms");
        assertTrue(count(c, type(ItemType.SHOES)) >= 2, "sport shoes");
        assertTrue(count(c, type(ItemType.SHOES).and(in(Season.WINTER))) >= 1, "winter sport shoes");
        assertTrue(count(c, type(ItemType.OUTERWEAR).and(fabric(Fabric.SYNTHETIC)).and(named("jacket"))
                .and(in(Season.AUTUMN, Season.WINTER))) >= 1, "synthetic jacket");
    }

    @Test
    void eveningCapsule() {
        List<Item> c = capsule(Capsule.EVENING);
        assertTrue(count(c, type(ItemType.DRESS)) >= 2, "evening dresses");
        assertTrue(count(c, type(ItemType.DRESS).and(fabric(Fabric.SILK)).and(in(Season.SUMMER))) >= 1,
                "silk summer dress");
        assertTrue(count(c, type(ItemType.DRESS).and(fabric(Fabric.WOOL)).and(in(Season.WINTER))) >= 1,
                "wool winter dress");
        assertTrue(count(c, type(ItemType.SHOES)) >= 2, "evening shoes");
        assertTrue(count(c, type(ItemType.OUTERWEAR).and(fabric(Fabric.WOOL)).and(named("coat"))) >= 1,
                "evening wool coat");
        assertTrue(count(c, type(ItemType.ACCESSORY)) >= 2, "evening accessories");
    }

    @Test
    void everyCapsuleHasSummerAndWinterThings() {
        for (Capsule capsule : Capsule.values()) {
            List<Item> c = capsule(capsule);
            assertTrue(count(c, in(Season.SUMMER)) >= 1, capsule + " summer");
            assertTrue(count(c, in(Season.WINTER)) >= 1, capsule + " winter");
        }
    }

    @Test
    void neutralsDominateEveryCapsuleAndItsAccentsAreFriends() {
        ColourRule rule = new ColourRule();
        for (Capsule capsule : Capsule.values()) {
            List<Item> c = capsule(capsule);
            long neutral = count(c, i -> i.colour().neutral());
            assertTrue(neutral * 2 > c.size(), capsule + ": " + neutral + " neutral of " + c.size());
            List<Item> accents = c.stream()
                    .filter(i -> !i.colour().neutral() && i.colour() != Colour.UNKNOWN).toList();
            assertTrue(accents.size() >= 2, capsule + " has a few accents");
            for (Item accent : accents) {
                boolean hasFriend = accents.stream().anyMatch(other -> other.colour() != accent.colour()
                        && rule.judge(accent, other).code() == ReasonCode.FRIENDS);
                assertTrue(hasFriend, capsule + ": accent " + accent.id() + " has no friend colour in its capsule");
            }
        }
    }

    /** The capsule an item belongs to is its id prefix (DECISIONS.md, WR-038). */
    private enum Capsule {
        OFFICE("office-", EnumSet.of(Formality.SMART_CASUAL, Formality.BUSINESS)),
        WEEKEND("weekend-", EnumSet.of(Formality.CASUAL, Formality.SMART_CASUAL)),
        SPORT("sport-", EnumSet.of(Formality.SPORT, Formality.CASUAL)),
        EVENING("evening-", EnumSet.of(Formality.BUSINESS, Formality.FORMAL));

        final String prefix;
        final Set<Formality> formalities;

        Capsule(String prefix, Set<Formality> formalities) {
            this.prefix = prefix;
            this.formalities = formalities;
        }

        static Capsule of(Item item) {
            return Arrays.stream(values()).filter(c -> item.id().value().startsWith(c.prefix)).findFirst()
                    .orElseThrow(() -> new AssertionError("no capsule prefix: " + item.id()));
        }
    }

    private static List<Item> capsule(Capsule capsule) {
        return ITEMS.stream().filter(i -> Capsule.of(i) == capsule).toList();
    }

    private static Predicate<Item> type(ItemType type) {
        return i -> i.type() == type;
    }

    private static Predicate<Item> fabric(Fabric... fabrics) {
        Set<Fabric> set = Set.of(fabrics);
        return i -> set.contains(i.fabric());
    }

    private static Predicate<Item> in(Season... seasons) {
        return i -> i.seasons().containsAll(Set.of(seasons));
    }

    private static Predicate<Item> named(String word) {
        return i -> i.name().toLowerCase().contains(word);
    }

    private static void assertAtLeast(int min, ItemType type) {
        long n = count(type(type));
        assertTrue(n >= min, type + ": " + n + " < " + min);
    }

    private static long count(Predicate<Item> p) {
        return count(ITEMS, p);
    }

    private static long count(List<Item> items, Predicate<Item> p) {
        return items.stream().filter(p).count();
    }

    private static <E extends Enum<E>> Set<E> known(E[] all, E unknown) {
        return Arrays.stream(all).filter(e -> e != unknown).collect(Collectors.toSet());
    }

    private static <E> Set<E> used(java.util.function.Function<Item, E> field) {
        return ITEMS.stream().map(field).filter(e -> !e.toString().equals("UNKNOWN")).collect(Collectors.toSet());
    }
}
