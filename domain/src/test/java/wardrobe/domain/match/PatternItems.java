package wardrobe.domain.match;

import java.util.Set;
import wardrobe.domain.Colour;
import wardrobe.domain.Fabric;
import wardrobe.domain.Formality;
import wardrobe.domain.Item;
import wardrobe.domain.ItemId;
import wardrobe.domain.ItemStatus;
import wardrobe.domain.ItemType;
import wardrobe.domain.Pattern;
import wardrobe.domain.Season;

/** Invented items that differ only by id and pattern (no real brands, no real person's things). */
final class PatternItems {
    private PatternItems() {
    }

    static Item item(String id, Pattern pattern) {
        return new Item(new ItemId(id), "test " + id, ItemType.TOP, Colour.GREY, pattern, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, ItemStatus.CLEAN);
    }
}
