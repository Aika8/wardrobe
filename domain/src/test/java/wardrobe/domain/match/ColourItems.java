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

/** Invented items that differ only by id and colour (no real brands, no real person's things). */
final class ColourItems {
    private ColourItems() {
    }

    static Item item(String id, Colour colour) {
        return new Item(new ItemId(id), "test " + id, ItemType.TOP, colour, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, ItemStatus.CLEAN);
    }
}
