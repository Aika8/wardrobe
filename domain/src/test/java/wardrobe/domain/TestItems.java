package wardrobe.domain;

import java.util.Set;

/** Invented items for tests (no real brands, no real person's things). */
public final class TestItems {
    private TestItems() {
    }

    public static Item item(String id, Formality formality) {
        return item(id, formality, ItemStatus.CLEAN);
    }

    public static Item item(String id, ItemStatus status) {
        return item(id, Formality.CASUAL, status);
    }

    public static Item item(String id, Formality formality, ItemStatus status) {
        return new Item(new ItemId(id), "test " + id, ItemType.TOP, Colour.WHITE, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), formality, status);
    }
}
