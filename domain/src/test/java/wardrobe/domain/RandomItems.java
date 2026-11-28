package wardrobe.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Invented random items for property tests. Always seeded: the same seed gives the same items (never the clock, never
 * an unseeded {@link Random}).
 */
public final class RandomItems {
    private RandomItems() {
    }

    /**
     * One item: every enum field uniform over all its values ({@code UNKNOWN} included for colour, pattern and fabric),
     * 1–4 distinct seasons, status CLEAN with probability 7/10, else one of the other three.
     */
    public static Item random(Random rnd, String id) {
        return new Item(new ItemId(id), "random " + id, pick(rnd, ItemType.values()), pick(rnd, Colour.values()),
                pick(rnd, Pattern.values()), pick(rnd, Fabric.values()), seasons(rnd), pick(rnd, Formality.values()),
                status(rnd));
    }

    /** {@code n} items with ids {@code r0 … r(n-1)}, drawn from one {@code new Random(seed)}. */
    public static List<Item> catalogue(long seed, int n) {
        Random rnd = new Random(seed);
        List<Item> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            items.add(random(rnd, "r" + i));
        }
        return List.copyOf(items);
    }

    private static <T> T pick(Random rnd, T[] values) {
        return values[rnd.nextInt(values.length)];
    }

    private static Set<Season> seasons(Random rnd) {
        List<Season> all = new ArrayList<>(List.of(Season.values()));
        Collections.shuffle(all, rnd);
        int count = 1 + rnd.nextInt(all.size());
        return EnumSet.copyOf(all.subList(0, count));
    }

    private static ItemStatus status(Random rnd) {
        if (rnd.nextInt(10) < 7) {
            return ItemStatus.CLEAN;
        }
        ItemStatus[] others = {ItemStatus.IN_WASH, ItemStatus.IN_REPAIR, ItemStatus.ON_TRIP};
        return pick(rnd, others);
    }
}
