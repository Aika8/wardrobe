package wardrobe.domain.match;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

/** Invented items that differ only by id and seasons (no real brands, no real person's things). */
final class SeasonItems {
    private SeasonItems() {
    }

    static Item item(String id, Set<Season> seasons) {
        return new Item(new ItemId(id), "test " + id, ItemType.TOP, Colour.BLACK, Pattern.PLAIN, Fabric.COTTON,
                seasons, Formality.CASUAL, ItemStatus.CLEAN);
    }

    /** Every non-empty subset of {@link Season}: 2^4 - 1 = 15 sets. */
    static List<Set<Season>> nonEmptySeasonSets() {
        Season[] all = Season.values();
        List<Set<Season>> sets = new ArrayList<>();
        for (int mask = 1; mask < (1 << all.length); mask++) {
            EnumSet<Season> set = EnumSet.noneOf(Season.class);
            for (int i = 0; i < all.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    set.add(all[i]);
                }
            }
            sets.add(set);
        }
        return sets;
    }
}
