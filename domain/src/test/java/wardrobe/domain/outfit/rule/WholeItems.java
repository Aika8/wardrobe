package wardrobe.domain.outfit.rule;

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

/** Invented items for the whole-outfit rule tests: only id, type and colour matter. */
final class WholeItems {
    private WholeItems() {
    }

    static Item item(String id, ItemType type, Colour colour) {
        return new Item(new ItemId(id), "test " + id, type, colour, Pattern.PLAIN, Fabric.COTTON,
                Set.of(Season.SUMMER), Formality.CASUAL, ItemStatus.CLEAN);
    }

    static ItemId id(String value) {
        return new ItemId(value);
    }
}
