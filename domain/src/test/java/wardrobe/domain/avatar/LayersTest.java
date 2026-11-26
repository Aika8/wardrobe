package wardrobe.domain.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import wardrobe.domain.outfit.Outfit;

/** Drawing order of an outfit on the avatar. Items are invented (no brands, no real things). */
class LayersTest {

    static Item item(String id, ItemType type) {
        return item(id, type, Colour.BLACK, ItemStatus.CLEAN);
    }

    static Item item(String id, ItemType type, Colour colour, ItemStatus status) {
        return new Item(new ItemId(id), "test " + id, type, colour, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, status);
    }

    static List<Item> order(Item... items) {
        return Layers.order(new Outfit(List.of(items)));
    }

    @Test
    void separatesAreDrawnFromShoesToAccessories() {
        Item x = item("x", ItemType.ACCESSORY);
        Item o = item("o", ItemType.OUTERWEAR);
        Item t = item("t", ItemType.TOP);
        Item b = item("b", ItemType.BOTTOM);
        Item s = item("s", ItemType.SHOES);
        assertEquals(List.of(s, b, t, o, x), order(x, o, t, b, s));
    }

    @Test
    void aDressTakesThePlaceOfTheTop() {
        Item d = item("d", ItemType.DRESS);
        Item s = item("s", ItemType.SHOES);
        Item o = item("o", ItemType.OUTERWEAR);
        Item x = item("x", ItemType.ACCESSORY);
        assertEquals(List.of(s, d, o, x), order(o, x, d, s));
    }

    @Test
    void accessoriesKeepTheirInputOrder() {
        Item x1 = item("x1", ItemType.ACCESSORY);
        Item x2 = item("x2", ItemType.ACCESSORY);
        Item x3 = item("x3", ItemType.ACCESSORY);
        Item t = item("t", ItemType.TOP);
        assertEquals(List.of(t, x3, x1, x2), order(x3, x1, t, x2));
    }

    @Test
    void theOrderIsTheSameForEveryInputPermutation() {
        Item s = item("s", ItemType.SHOES);
        Item b = item("b", ItemType.BOTTOM);
        Item t = item("t", ItemType.TOP);
        Item o = item("o", ItemType.OUTERWEAR);
        Item x = item("x", ItemType.ACCESSORY);
        List<Item> expected = List.of(s, b, t, o, x);
        List<List<Item>> perms = permutations(List.of(s, b, t, o, x));
        assertEquals(120, perms.size());
        for (List<Item> p : perms) {
            assertEquals(expected, Layers.order(new Outfit(p)), "input " + p);
        }
    }

    @Test
    void everyTypeHasItsOwnLayerButTopAndDressShareOne() {
        assertEquals(List.of(0, 1, 2, 2, 3, 4), List.of(ItemType.SHOES, ItemType.BOTTOM, ItemType.TOP,
                ItemType.DRESS, ItemType.OUTERWEAR, ItemType.ACCESSORY).stream().map(Layers::layer).toList());
    }

    @Test
    void anEmptyOutfitHasNothingToDraw() {
        assertEquals(List.of(), order());
    }

    @Test
    void theInputIsNotChangedAndTheResultIsImmutable() {
        Item x = item("x", ItemType.ACCESSORY);
        Item s = item("s", ItemType.SHOES);
        Outfit outfit = new Outfit(List.of(x, s));
        List<Item> drawn = Layers.order(outfit);
        assertEquals(List.of(x, s), outfit.items());
        assertThrows(UnsupportedOperationException.class, () -> drawn.add(x));
    }

    @Test
    void twoOfAKindIsRefusedWithTheProblem() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> order(item("t1", ItemType.TOP), item("t2", ItemType.TOP)));
        assertTrue(e.getMessage().contains("test t1 and test t2 are both TOP"), e.getMessage());
    }

    @Test
    void aDressWithSeparatesIsRefusedListingEveryProblem() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> order(item("d", ItemType.DRESS), item("t", ItemType.TOP), item("b", ItemType.BOTTOM),
                        item("s1", ItemType.SHOES), item("s2", ItemType.SHOES)));
        String m = e.getMessage();
        assertTrue(m.contains("test d is a dress and is not worn with test t (TOP)"), m);
        assertTrue(m.contains("test d is a dress and is not worn with test b (BOTTOM)"), m);
        assertTrue(m.contains("test s1 and test s2 are both SHOES"), m);
    }

    @Test
    void dirtyItemsAndClashesAreStillDrawn() {
        Item t = item("t", ItemType.TOP, Colour.RED, ItemStatus.IN_WASH);
        Item b = item("b", ItemType.BOTTOM, Colour.GREEN, ItemStatus.CLEAN);
        assertEquals(List.of(b, t), order(t, b));
    }

    static List<List<Item>> permutations(List<Item> items) {
        if (items.isEmpty()) {
            return List.of(List.of());
        }
        List<List<Item>> out = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<Item> rest = new java.util.ArrayList<>(items);
            Item head = rest.remove(i);
            for (List<Item> p : permutations(rest)) {
                List<Item> withHead = new java.util.ArrayList<>();
                withHead.add(head);
                withHead.addAll(p);
                out.add(withHead);
            }
        }
        return out;
    }
}
